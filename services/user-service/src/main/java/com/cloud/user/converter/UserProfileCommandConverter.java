package com.cloud.user.converter;

import com.cloud.common.domain.dto.user.UserProfileUpsertDTO;
import com.cloud.common.messaging.event.UserProfileSyncEvent;
import com.cloud.user.module.dto.UserProfileUpdateDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface UserProfileCommandConverter {

  UserProfileUpsertDTO toUpsertDTO(UserProfileUpdateDTO updateDTO);

  UserProfileUpsertDTO toUpsertDTO(UserProfileSyncEvent event);
}
