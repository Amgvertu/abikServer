package info.prorabka.abik.server.team.entity;

/**
 * Роль участника в команде.
 */
public enum MemberRole {
    /** Владелец — ровно один на команду. */
    OWNER,
    /** Модератор — может быть много. */
    MODERATOR,
    /** Обычный участник. */
    MEMBER
}