package info.prorabka.abik.server.charge.repository;

import info.prorabka.abik.server.charge.entity.ChargeTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChargeTemplateRepository extends JpaRepository<ChargeTemplate, Long> {

    /** Все шаблоны команды (включая неактивные), свежие сверху. */
    List<ChargeTemplate> findByTeamIdOrderByCreatedAtDesc(Long teamId);

    /** Активные шаблоны команды. */
    List<ChargeTemplate> findByTeamIdAndActiveOrderByCreatedAtDesc(Long teamId, Boolean active);

    /** Активные систематические шаблоны всех команд — для планировщика. */
    List<ChargeTemplate> findByActiveTrue();
}