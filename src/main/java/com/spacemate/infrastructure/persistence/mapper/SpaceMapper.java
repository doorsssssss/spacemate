package com.spacemate.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spacemate.domain.entity.Space;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SpaceMapper extends BaseMapper<Space> {
}


