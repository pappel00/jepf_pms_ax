package com.platform.module.portal.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.core.common.response.PageInfo;
import com.platform.framework.core.common.response.PagedResult;
import com.platform.module.portal.domain.DataService;
import com.platform.module.portal.dto.DataServiceDto;
import com.platform.module.portal.exception.PortalModuleErrorCode;
import com.platform.module.portal.repository.DataServiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 클래스명이 Spring의 org.springframework.stereotype.Service 애너테이션과 동일해서 헷갈릴 수 있지만,
 * @Service 는 애너테이션이고 DataServiceService는 이 애너테이션이 붙는 우리 클래스명이라 문제없다
 * (Company/Department/Portal 등과 동일한 "[도메인]Service" 네이밍 규칙을 따른 것 뿐이다).
 */
@Service
@Transactional(readOnly = true)
public class DataServiceService {

    private final DataServiceRepository dataServiceRepository;

    public DataServiceService(DataServiceRepository dataServiceRepository) {
        this.dataServiceRepository = dataServiceRepository;
    }

    @Transactional
    public DataServiceDto.Response create(DataServiceDto.CreateRequest request) {
        if (dataServiceRepository.existsByDataServiceCode(request.dataServiceCode())) {
            throw new BusinessException(PortalModuleErrorCode.DATA_SERVICE_CODE_DUPLICATED,
                    "이미 존재하는 데이터서비스 코드입니다: " + request.dataServiceCode());
        }
        DataService dataService = DataService.create(
                request.dataServiceCode(), request.dataServiceName(), request.httpMethod(), request.url());
        return DataServiceDto.Response.from(dataServiceRepository.saveAndFlush(dataService));
    }

    public DataServiceDto.Response getById(String id) {
        return DataServiceDto.Response.from(findActiveOrThrow(id));
    }

    public PagedResult<DataServiceDto.Response> getList(Pageable pageable) {
        Page<DataService> page = dataServiceRepository.findAll(pageable);
        return new PagedResult<>(page.map(DataServiceDto.Response::from).getContent(), PageInfo.of(page));
    }

    @Transactional
    public DataServiceDto.Response update(String id, DataServiceDto.UpdateRequest request) {
        DataService dataService = findActiveOrThrow(id);
        dataService.update(request.dataServiceName(), request.httpMethod(), request.url());
        if ("Y".equals(request.useYn())) {
            dataService.activate();
        } else {
            dataService.deactivate();
        }
        return DataServiceDto.Response.from(dataService);
    }

    @Transactional
    public void delete(String id) {
        findActiveOrThrow(id).markDeleted();
    }

    private DataService findActiveOrThrow(String id) {
        return dataServiceRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(PortalModuleErrorCode.DATA_SERVICE_NOT_FOUND,
                        "존재하지 않는 데이터서비스입니다: " + id));
    }
}
