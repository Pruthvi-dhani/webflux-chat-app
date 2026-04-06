package org.chatapp.model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("chat_room_members")
public class ChatRoomMember {
    @Id
    private Long id;

    @Column("room_id")
    private Long roomId;

    @Column("user_id")
    private Long userId;

    @CreatedDate
    @Column("joined_at")
    private LocalDateTime joinedAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
