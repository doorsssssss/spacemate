package com.spacemate.modules.client.service;

import com.spacemate.common.api.PageResponse;
import com.spacemate.modules.client.dto.response.ClientSeatResponse;
import com.spacemate.domain.entity.Seat;
import java.time.LocalDateTime;

public interface ClientSeatService {

    PageResponse<ClientSeatResponse> available(
        Long spaceId,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Integer hasSocket,
        Integer isQuiet,
        long page,
        long size
    );

    ClientSeatResponse detail(Long id);

    Seat requireActiveSeat(Long id);
}



