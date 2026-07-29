package com.platform.module.portal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.portal.domain.Menu;
import com.platform.module.portal.domain.MenuPage;
import com.platform.module.portal.dto.MenuDto;
import com.platform.module.portal.repository.MenuPageRepository;
import com.platform.module.portal.repository.MenuRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private MenuPageRepository menuPageRepository;

    @Mock
    private PageRoleService pageRoleService;

    private MenuService service() {
        return new MenuService(menuRepository, menuPageRepository, pageRoleService);
    }

    @Test
    void createFailsWhenMenuCodeDuplicated() {
        when(menuRepository.existsByMenuCode("USR_MGT")).thenReturn(true);

        var request = new MenuDto.CreateRequest("USR_MGT", "사용자 관리", "APP_1", null, 1, null);

        assertThatThrownBy(() -> service().create(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getByIdThrowsWhenNotFound() {
        when(menuRepository.findByIdAndDeletedYn("MNU_unknown", "N")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getById("MNU_unknown"))
                .isInstanceOf(TenantException.class);
    }

    @Test
    void getByIdReturnsResponseWhenFound() {
        Menu menu = Menu.create("USR_MGT", "사용자 관리", "APP_1", null, 1, null);
        when(menuRepository.findByIdAndDeletedYn(menu.getId(), "N")).thenReturn(Optional.of(menu));

        MenuDto.Response response = service().getById(menu.getId());

        assertThat(response.menuCode()).isEqualTo("USR_MGT");
    }

    @Test
    void getTreeBuildsHierarchyFromFlatList() {
        Menu root = Menu.create("ROOT", "최상위", "APP_1", null, 1, null);
        Menu child = Menu.create("CHILD", "하위", "APP_1", root.getId(), 1, null);
        Menu grandchild = Menu.create("GRANDCHILD", "손자", "APP_1", child.getId(), 1, null);

        when(menuRepository.findAllByApplicationIdAndDeletedYnOrderBySortOrderAsc("APP_1", "N"))
                .thenReturn(List.of(root, child, grandchild));
        when(menuPageRepository.findAllByMenuIdOrderBySortOrderAsc(anyString())).thenReturn(List.of());

        List<MenuDto.TreeNode> tree = service().getTree("APP_1");

        assertThat(tree).hasSize(1);
        MenuDto.TreeNode rootNode = tree.get(0);
        assertThat(rootNode.menuCode()).isEqualTo("ROOT");
        assertThat(rootNode.children()).hasSize(1);

        MenuDto.TreeNode childNode = rootNode.children().get(0);
        assertThat(childNode.menuCode()).isEqualTo("CHILD");
        assertThat(childNode.children()).hasSize(1);
        assertThat(childNode.children().get(0).menuCode()).isEqualTo("GRANDCHILD");
        assertThat(childNode.children().get(0).children()).isEmpty();
    }

    /**
     * N:M으로 바뀐 뒤의 동작: 폴더 자체는 항상 트리에 남고, 그 안에 매핑된 페이지 중 접근
     * 불가능한 것만 pageIds에서 빠진다(예전처럼 폴더 서브트리 전체가 사라지지 않는다).
     */
    @Test
    void getTreeKeepsFolderButFiltersInaccessiblePageIds() {
        Menu root = Menu.create("ROOT", "최상위", "APP_1", null, 1, null);
        Menu folder = Menu.create("FOLDER", "폴더", "APP_1", root.getId(), 1, null);

        when(menuRepository.findAllByApplicationIdAndDeletedYnOrderBySortOrderAsc("APP_1", "N"))
                .thenReturn(List.of(root, folder));
        when(menuPageRepository.findAllByMenuIdOrderBySortOrderAsc(root.getId())).thenReturn(List.of());
        when(menuPageRepository.findAllByMenuIdOrderBySortOrderAsc(folder.getId()))
                .thenReturn(List.of(MenuPage.create(folder.getId(), "PGE_LOCKED", 0)));
        when(pageRoleService.isAccessible("PGE_LOCKED", Set.of())).thenReturn(false);

        List<MenuDto.TreeNode> tree = service().getTree("APP_1");

        assertThat(tree).hasSize(1);
        MenuDto.TreeNode rootNode = tree.get(0);
        assertThat(rootNode.children()).hasSize(1);
        MenuDto.TreeNode folderNode = rootNode.children().get(0);
        assertThat(folderNode.menuCode()).isEqualTo("FOLDER");
        assertThat(folderNode.pageIds()).isEmpty();
    }

    /**
     * 실제로 겪은 버그의 회귀 테스트: parentMenuId 로 빈 문자열("")을 보내면 Menu.create() 가
     * null 로 정규화해야 하고, 그 결과 최상위 메뉴 판정과 트리 조립이 정상적으로 동작해야 한다
     * (예전엔 ""가 null 체크를 통과하지 못해 트리 전체가 비어 나왔었다).
     */
    @Test
    void getTreeTreatsEmptyStringParentAsNull() {
        Menu menu1 = Menu.create("USR_MGT_MENU1", "테스트 메뉴", "APP_1", "", 0, null);
        Menu menu1_1 = Menu.create("USR_MGT_MENU1-1", "1-1", "APP_1", menu1.getId(), 1, "");
        Menu menu2 = Menu.create("USR_MGT_MENU2", "2", "APP_1", "", 1, "");

        // 엔티티 레벨에서 이미 null로 정규화됐어야 한다.
        assertThat(menu1.getParentMenuId()).isNull();
        assertThat(menu1_1.getLinkUrl()).isNull();
        assertThat(menu2.getParentMenuId()).isNull();

        when(menuRepository.findAllByApplicationIdAndDeletedYnOrderBySortOrderAsc("APP_1", "N"))
                .thenReturn(List.of(menu1, menu1_1, menu2));
        when(menuPageRepository.findAllByMenuIdOrderBySortOrderAsc(anyString())).thenReturn(List.of());

        List<MenuDto.TreeNode> tree = service().getTree("APP_1");

        // MENU1(자식 MENU1-1 포함)과 MENU2, 최상위에 2개가 있어야 한다.
        assertThat(tree).hasSize(2);
        MenuDto.TreeNode menu1Node = tree.stream()
                .filter(node -> node.menuCode().equals("USR_MGT_MENU1"))
                .findFirst().orElseThrow();
        assertThat(menu1Node.children()).hasSize(1);
        assertThat(menu1Node.children().get(0).menuCode()).isEqualTo("USR_MGT_MENU1-1");
    }
}
