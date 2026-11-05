package info.prorabka.abik.server.offline.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import info.prorabka.abik.server.charge.dto.ChargeCreateManualRequest;
import info.prorabka.abik.server.charge.dto.ChargeGroupRequest;
import info.prorabka.abik.server.charge.dto.ChargeSubgroupRequest;
import info.prorabka.abik.server.charge.service.ChargeGroupService;
import info.prorabka.abik.server.charge.service.ChargeService;
import info.prorabka.abik.server.common.exception.BadRequestException;
import info.prorabka.abik.server.common.exception.ConflictException;
import info.prorabka.abik.server.event.dto.LineupSaveRequest;
import info.prorabka.abik.server.event.service.EventService;
import info.prorabka.abik.server.expense.dto.ExpenseRequest;
import info.prorabka.abik.server.expense.service.ExpenseService;
import info.prorabka.abik.server.offline.dto.*;
import info.prorabka.abik.server.offline.entity.OfflineOperation;
import info.prorabka.abik.server.offline.repository.OfflineOperationRepository;
import info.prorabka.abik.server.payment.dto.PaymentCreateRequest;
import info.prorabka.abik.server.payment.service.PaymentService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OfflineOperationService {
    public static final String EXPENSE_CREATE = "EXPENSE_CREATE";
    public static final String PAYMENT_CREATE = "PAYMENT_CREATE";
    public static final String CHARGE_CREATE = "CHARGE_CREATE";
    public static final String EVENT_LINEUP_SAVE = "EVENT_LINEUP_SAVE";
    public static final String EVENT_SWEATERS_SAVE = "EVENT_SWEATERS_SAVE";

    private final OfflineOperationRepository repository;
    private final ExpenseService expenseService;
    private final PaymentService paymentService;
    private final ChargeService chargeService;
    private final ChargeGroupService chargeGroupService;
    private final EventService eventService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @PersistenceContext
    private EntityManager entityManager;

    public OfflineOperationService(OfflineOperationRepository repository,
                                   ExpenseService expenseService,
                                   PaymentService paymentService,
                                   ChargeService chargeService,
                                   ChargeGroupService chargeGroupService,
                                   EventService eventService,
                                   ObjectMapper objectMapper,
                                   Validator validator) {
        this.repository = repository;
        this.expenseService = expenseService;
        this.paymentService = paymentService;
        this.chargeService = chargeService;
        this.chargeGroupService = chargeGroupService;
        this.eventService = eventService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    /**
     * Выполняет операцию и сохраняет её ответ в одной транзакции.
     * Advisory lock сериализует одновременные повторы одного UUID в PostgreSQL:
     * второй запрос увидит уже закоммиченный ответ и не создаст дубль.
     */
    @Transactional
    public JsonNode execute(Long teamId, Long actor, OfflineOperationRequest request) {
        String lockKey = actor + ":" + request.operationId();
        entityManager.createNativeQuery(
                        "SELECT pg_advisory_xact_lock(hashtextextended(?1, 0))")
                .setParameter(1, lockKey)
                .getResultList();

        String requestHash = hash(teamId + "\n" + request.type() + "\n"
                + request.payload().toString() + "\n" + request.expectedEventRevision());
        var previous = repository.findByUserIdAndOperationId(actor, request.operationId());
        if (previous.isPresent()) {
            OfflineOperation saved = previous.get();
            if (!saved.getTeamId().equals(teamId) || !saved.getRequestHash().equals(requestHash)) {
                throw new ConflictException("Этот operationId уже использован для другой операции");
            }
            return readJson(saved.getResponseJson());
        }

        Object result = switch (request.type()) {
            case EXPENSE_CREATE -> {
                ExpenseRequest body = payload(request.payload(), ExpenseRequest.class);
                validate(body);
                yield expenseService.create(teamId, actor, body);
            }
            case PAYMENT_CREATE -> {
                PaymentCreateRequest body = payload(request.payload(), PaymentCreateRequest.class);
                validate(body);
                yield paymentService.create(teamId, actor, body);
            }
            case CHARGE_CREATE -> createCharge(teamId, actor,
                    payload(request.payload(), OfflineChargeCreateRequest.class));
            case EVENT_LINEUP_SAVE -> {
                OfflineLineupRequest body = payload(request.payload(), OfflineLineupRequest.class);
                requireEventId(body.eventId());
                eventService.requireOfflineLineupRevision(teamId, body.eventId(),
                        request.expectedEventRevision());
                eventService.setLineup(teamId, body.eventId(), actor,
                        new LineupSaveRequest(body.slots()));
                yield Map.of("message", "Состав синхронизирован");
            }
            case EVENT_SWEATERS_SAVE -> {
                OfflineSweatersRequest body = payload(request.payload(), OfflineSweatersRequest.class);
                requireEventId(body.eventId());
                eventService.requireOfflineSweaterRevision(teamId, body.eventId(),
                        request.expectedEventRevision());
                eventService.setSweaterColors(teamId, body.eventId(), actor, body.items());
                yield Map.of("message", "Цвета синхронизированы");
            }
            default -> throw new BadRequestException("Неизвестный тип офлайн-операции: " + request.type());
        };

        String responseJson = objectMapper.valueToTree(result == null ? Map.of() : result).toString();
        repository.save(OfflineOperation.builder()
                .operationId(request.operationId())
                .userId(actor)
                .teamId(teamId)
                .operationType(request.type())
                .requestHash(requestHash)
                .responseJson(responseJson)
                .build());
        return readJson(responseJson);
    }

    private Object createCharge(Long teamId, Long actor, OfflineChargeCreateRequest body) {
        Long groupId = body.groupId();
        if (groupId == null) {
            String name = requiredName(body.groupName(), "Укажите название назначения");
            groupId = chargeGroupService.listGroups(teamId, actor).stream()
                    .filter(g -> g.name().equalsIgnoreCase(name))
                    .map(g -> g.id())
                    .findFirst()
                    .orElseGet(() -> chargeGroupService.createGroup(teamId, actor,
                            new ChargeGroupRequest(name)).id());
        }

        Long subgroupId = body.subgroupId();
        if (subgroupId == null && body.subgroupName() != null && !body.subgroupName().isBlank()) {
            String name = body.subgroupName().trim();
            Long parentId = groupId;
            subgroupId = chargeGroupService.listSubgroups(teamId, parentId, actor).stream()
                    .filter(g -> g.name().equalsIgnoreCase(name))
                    .map(g -> g.id())
                    .findFirst()
                    .orElseGet(() -> chargeGroupService.createSubgroup(teamId, parentId, actor,
                            new ChargeSubgroupRequest(name)).id());
        }

        ChargeCreateManualRequest request = new ChargeCreateManualRequest(
                groupId, subgroupId, body.userIds(), body.amountKopecks(),
                body.description(), body.dueDate());
        validate(request);
        return chargeService.createManual(teamId, actor, request);
    }

    private String requiredName(String value, String message) {
        if (value == null || value.isBlank()) throw new BadRequestException(message);
        return value.trim();
    }

    private void requireEventId(Long eventId) {
        if (eventId == null) throw new BadRequestException("Не указано событие");
    }

    private <T> T payload(JsonNode node, Class<T> type) {
        try {
            return objectMapper.treeToValue(node, type);
        } catch (JacksonException | IllegalArgumentException e) {
            throw new BadRequestException("Некорректные данные офлайн-операции: " + e.getMessage());
        }
    }

    private <T> void validate(T value) {
        Set<ConstraintViolation<T>> violations = validator.validate(value);
        if (!violations.isEmpty()) {
            String message = violations.stream().map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; "));
            throw new BadRequestException(message);
        }
    }

    private JsonNode readJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (JacksonException e) {
            throw new IllegalStateException("Повреждён сохранённый ответ офлайн-операции", e);
        }
    }

    private static String hash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
