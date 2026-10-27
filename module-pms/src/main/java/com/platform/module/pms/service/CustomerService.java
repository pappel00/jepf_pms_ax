package com.platform.module.pms.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.core.common.response.PageInfo;
import com.platform.framework.core.common.response.PagedResult;
import com.platform.module.pms.domain.Customer;
import com.platform.module.pms.domain.CustomerContact;
import com.platform.module.pms.dto.CustomerDto;
import com.platform.module.pms.exception.PmsErrorCode;
import com.platform.module.pms.repository.CustomerContactRepository;
import com.platform.module.pms.repository.CustomerRepository;
import com.platform.module.pms.repository.ProjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 고객사(발주사) 마스터. 프로젝트에서 사용 중이면 삭제할 수 없다(거래 종료로 변경). */
@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerContactRepository contactRepository;
    private final ProjectRepository projectRepository;

    public CustomerService(CustomerRepository customerRepository, CustomerContactRepository contactRepository,
                           ProjectRepository projectRepository) {
        this.customerRepository = customerRepository;
        this.contactRepository = contactRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public CustomerDto.Response create(CustomerDto.Request request) {
        String code = PmsSupport.nextCode("CUS-",
                customerRepository.findAll().stream().map(Customer::getCustomerCode).toList(), 3);
        Customer customer = apply(Customer.create().withCustomerCode(code), request);
        return CustomerDto.Response.from(customerRepository.saveAndFlush(customer));
    }

    public CustomerDto.Response getById(String id) {
        return CustomerDto.Response.from(findActiveOrThrow(id));
    }

    public PagedResult<CustomerDto.Response> getList(Pageable pageable) {
        Page<Customer> page = customerRepository.findAllByDeletedYn("N", pageable);
        return new PagedResult<>(page.map(CustomerDto.Response::from).getContent(), PageInfo.of(page));
    }

    @Transactional
    public CustomerDto.Response update(String id, CustomerDto.Request request) {
        return CustomerDto.Response.from(apply(findActiveOrThrow(id), request));
    }

    @Transactional
    public void delete(String id) {
        Customer customer = findActiveOrThrow(id);
        if (projectRepository.existsByCustomerIdAndDeletedYn(id, "N")) {
            throw new BusinessException(PmsErrorCode.CUSTOMER_IN_USE,
                    "프로젝트에서 사용 중인 고객사는 삭제할 수 없습니다. 거래 상태를 「거래 종료」로 바꾸세요.");
        }
        for (CustomerContact contact : contactRepository.findAllByCustomerIdAndDeletedYn(id, "N")) {
            contact.markDeleted();
        }
        customer.markDeleted();
    }

    private Customer apply(Customer c, CustomerDto.Request r) {
        return c.withCustomerName(r.customerName().trim())
                .withIndustryCode(PmsSupport.blankToNull(r.industryCode()))
                .withDealStatusCode(r.dealStatusCode())
                .withBizNo(PmsSupport.blankToNull(r.bizNo()))
                .withPhone(PmsSupport.blankToNull(r.phone()))
                .withAddress(PmsSupport.blankToNull(r.address()))
                .withHomepage(PmsSupport.blankToNull(r.homepage()))
                .withMemo(PmsSupport.blankToNull(r.memo()));
    }

    private Customer findActiveOrThrow(String id) {
        return customerRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(PmsErrorCode.CUSTOMER_NOT_FOUND, "존재하지 않는 고객사입니다: " + id));
    }
}
