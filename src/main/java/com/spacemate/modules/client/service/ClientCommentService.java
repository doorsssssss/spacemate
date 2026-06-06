package com.spacemate.modules.client.service;

import com.spacemate.common.api.PageResponse;
import com.spacemate.modules.client.dto.request.ClientCreateCommentRequest;
import com.spacemate.modules.client.dto.response.ClientCommentLikeResponse;
import com.spacemate.modules.client.dto.response.ClientCommentResponse;

public interface ClientCommentService {

    PageResponse<ClientCommentResponse> list(Long spaceId, String phone, long page, long size);

    ClientCommentResponse create(Long spaceId, String phone, ClientCreateCommentRequest request);

    ClientCommentLikeResponse like(Long commentId, String phone);

    ClientCommentLikeResponse unlike(Long commentId, String phone);
}