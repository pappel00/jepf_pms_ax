package com.platform.module.pms.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.InvalidRequestException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.core.common.response.ErrorDetail;
import com.platform.framework.core.common.response.PageInfo;
import com.platform.framework.core.common.response.PagedResult;
import com.platform.module.pms.domain.CustomerContact;
import com.platform.module.pms.dto.CustomerContactDto;
import com.platform.module.pms.exception.PmsErrorCode;
import com.platform.module.pms.repository.CustomerContactRepository;
import com.platform.module.pms.repository.CustomerRepository;
import com.platform.module.pms.repository.ProjectRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 고객사 담당자 마스터. 로그인 계정과 연결하지 않는다(고객사 담당자에게 PMS 비노출). */
@Service
@Transactional(readOnly = true)
public class CustomerContactService {

    private final CustomerContactRepository contactRepository;
    private final CustomerRepository customerRepository;
    private final ProjectRepository projectRepository;

    public CustomerContactService(CustomerContactRepository contactRepository, CustomerRepository customerRepository,
                                  ProjectRepository projectRepository) {
        this.contactRepository = contactRepository;
        this.customerRepository = customerRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public CustomerContactDto.Response create(CustomerContactDto.Request request) {
        validate(request);
        String code = PmsSupport.nextCode("CTC-",
                contactRepository.findAll().stream().map(CustomerContact::getContactCode).toList(), 3);
        CustomerContact contact = apply(CustomerContact.create().withContactCode(code), request);
        return CustomerContactDto.Response.from(contactRepository.saveAndFlush(contact));
    }

    public CustomerContactDto.Response getById(String id) {
        return CustomerContactDto.Response.from(findActiveOrThrow(id));
    }

    /** customerId 가 있으면 그 고객사의 담당자만. */
    public PagedResult<CustomerContactDto.Response> getList(String customerId, Pageable pageable) {
        Page<CustomerContact> page = customerId == null || customerId.isBlank()
                ? contactRepository.findAllByDeletedYn("N", pageable)
                : contactRepository.findAllByCustomerIdAndDeletedYn(customerId, "N", pageable);
        return new PagedResult<>(page.map(CustomerContactDto.Response::from).getContent(), PageInfo.of(page));
    }

    @Transactional
    public CustomerContactDto.Response update(String id, CustomerContactDto.Request request) {
        validate(request);
        CustomerContact contact = findActiveOrThrow(id);
        if (!contact.getCustomerId().equals(request.customerId())
                && projectRepository.existsByContactIdAndDeletedYn(id, "N")) {
            throw new BusinessException(PmsErrorCode.CONTACT_CUSTOMER_MISMATCH,
                    "프로젝트에서 사용 중인 담당자는 다른 고객사로 옮길 수 없습니다.");
        }
        return CustomerContactDto.Response.from(apply(contact, request));
    }

    @Transactional
    public void delete(String id) {
        CustomerContact contact = findActiveOrThrow(id);
        if (projectRepository.existsByContactIdAndDeletedYn(id, "N")) {
            throw new BusinessException(PmsErrorCode.CONTACT_IN_USE,
                    "프로젝트에서 사용 중인 담당자는 삭제할 수 없습니다. 사용 여부를 「미사용」으로 바꾸세요.");
        }
        contact.markDeleted();
    }

    private void validate(CustomerContactDto.Request r) {
        if (customerRepository.findByIdAndDeletedYn(r.customerId(), "N").isEmpty()) {
            throw new TenantException(PmsErrorCode.CUSTOMER_NOT_FOUND, "존재하지 않는 고객사입니다: " + r.customerId());
        }
        boolean noPhone = r.phone() == null || r.phone().isBlank();
        boolean noEmail = r.email() == null || r.email().isBlank();
        if (noPhone && noEmail) {
            throw new InvalidRequestException(PmsErrorCode.CONTACT_INVALID, "연락처 또는 이메일 중 하나는 필수입니다.",
                    List.of(new ErrorDetail("phone", r.phone(), "연락처 또는 이메일 중 하나는 필수입니다.")));
        }
    }

    private CustomerContact apply(CustomerContact c, CustomerContactDto.Request r) {
        return c.withCustomerId(r.customerId())
                .withContactName(r.contactName().trim())
                .withJobTitle(PmsSupport.blankToNull(r.jobTitle()))
                .withDeptName(PmsSupport.blankToNull(r.deptName()))
                .withPhone(PmsSupport.blankToNull(r.phone()))
                .withEmail(PmsSupport.blankToNull(r.email()))
                .withMemo(PmsSupport.blankToNull(r.memo()))
                .withUseYn("N".equals(r.useYn()) ? "N" : "Y");
    }

    private CustomerContact findActiveOrThrow(String id) {
        return contactRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(PmsErrorCode.CONTACT_NOT_FOUND, "존재하지 않는 담당자입니다: " + id));
    }
}
