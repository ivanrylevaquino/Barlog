package com.barlog.barlog_back.video;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VideoClipRepository extends JpaRepository<VideoClip, UUID> {

    Optional<VideoClip> findBySet_SetId(UUID setId);

    @EntityGraph(attributePaths = {"set"})
    List<VideoClip> findBySet_Session_User_UserId(UUID userId);

    List<VideoClip> findBySet_SetIdIn(Collection<UUID> setIds);

    Optional<VideoClip> findByVideoIdAndSet_Session_User_UserId(UUID videoId, UUID userId);
}
