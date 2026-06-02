package com.spacemate.modules.client.service;

import com.spacemate.modules.client.dto.response.ClientSpaceResponse;
import com.spacemate.domain.entity.Space;
import java.util.List;

public interface ClientSpaceService {

    List<ClientSpaceResponse> list();

    ClientSpaceResponse detail(Long id);

    Space requireActiveSpace(Long id);
}



