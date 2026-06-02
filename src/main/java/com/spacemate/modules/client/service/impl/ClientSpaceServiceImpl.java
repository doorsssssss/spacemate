package com.spacemate.modules.client.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spacemate.common.exception.BusinessException;
import com.spacemate.modules.client.dto.response.ClientSpaceResponse;
import com.spacemate.domain.entity.Space;
import com.spacemate.infrastructure.persistence.mapper.SpaceMapper;
import java.util.List;
import java.util.stream.Collectors;

import com.spacemate.modules.client.service.ClientSpaceService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class ClientSpaceServiceImpl implements ClientSpaceService {

    private final SpaceMapper spaceMapper;

    public ClientSpaceServiceImpl(SpaceMapper spaceMapper) {
        this.spaceMapper = spaceMapper;
    }

    public List<ClientSpaceResponse> list() {
        List<Space> spaces = spaceMapper.selectList(
            new LambdaQueryWrapper<Space>()
                .eq(Space::getStatus, 1)
                .orderByAsc(Space::getId)
        );
        return spaces.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ClientSpaceResponse detail(Long id) {
        Space space = spaceMapper.selectOne(new LambdaQueryWrapper<Space>()
            .eq(Space::getId, id)
            .eq(Space::getStatus, 1));
        if (space == null) {
            throw new BusinessException(404, "绌洪棿涓嶅瓨鍦?");
        }
        return toResponse(space);
    }

    public Space requireActiveSpace(Long id) {
        Space space = spaceMapper.selectOne(new LambdaQueryWrapper<Space>()
            .eq(Space::getId, id)
            .eq(Space::getStatus, 1));
        if (space == null) {
            throw new BusinessException(404, "绌洪棿涓嶅瓨鍦ㄦ垨宸插仠鐢?");
        }
        return space;
    }

    private ClientSpaceResponse toResponse(Space space) {
        ClientSpaceResponse response = new ClientSpaceResponse();
        BeanUtils.copyProperties(space, response);
        return response;
    }
}




