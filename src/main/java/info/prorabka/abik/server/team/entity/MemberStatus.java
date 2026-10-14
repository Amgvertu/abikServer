package info.prorabka.abik.server.team.entity;

/**
 * Статус участника в команде.
 */
public enum MemberStatus {
    /** Активен. */
    ACTIVE,
    /** Исключён владельцем/модератором. Долги сохраняются. */
    EXCLUDED,
    /** Вышел сам. */
    LEFT
}