package com.dms.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dms.auth.DataScope;
import com.dms.auth.LoginUser;
import com.dms.auth.UserContext;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

public abstract class BaseCrudController<T extends BaseEntity, M extends BaseMapper<T>> {
    @Autowired protected M mapper;
    private final Class<T> entityClass;

    protected BaseCrudController(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    protected abstract String[] keywordColumns();

    /** 数据范围列：默认检测实体是否存在 dealer_code 字段；可由子类覆盖（如 Dealer 用 code）。 */
    protected String scopeColumn() {
        TableInfo info = TableInfoHelper.getTableInfo(entityClass);
        if (info == null) {
            return null;
        }
        for (TableFieldInfo f : info.getFieldList()) {
            if ("dealer_code".equals(f.getColumn())) {
                return "dealer_code";
            }
        }
        return null;
    }

    private Field scopeField() {
        String col = scopeColumn();
        if (col == null) {
            return null;
        }
        TableInfo info = TableInfoHelper.getTableInfo(entityClass);
        String property = null;
        if (info != null) {
            for (TableFieldInfo f : info.getFieldList()) {
                if (col.equals(f.getColumn())) {
                    property = f.getProperty();
                    break;
                }
            }
        }
        if (property == null) {
            return null;
        }
        try {
            Field field = entityClass.getDeclaredField(property);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private boolean scoped() {
        LoginUser u = UserContext.get();
        return u != null && !u.networkWide() && scopeField() != null;
    }

    private void applyScope(QueryWrapper<T> q) {
        String col = scopeColumn();
        if (col != null && scoped()) {
            q.eq(col, UserContext.get().getDealerCode());
        }
    }

    private void checkScope(T existing) {
        Field f = scopeField();
        if (f == null || !scoped() || existing == null) {
            return;
        }
        try {
            Object v = f.get(existing);
            DataScope.check(v == null ? null : v.toString());
        } catch (IllegalAccessException e) {
            throw new BizException("无权访问其他经销商数据");
        }
    }

    private void forceScope(T entity) {
        Field f = scopeField();
        if (f == null || !scoped()) {
            return;
        }
        try {
            f.set(entity, UserContext.get().getDealerCode());
        } catch (IllegalAccessException e) {
            throw new BizException("无权访问其他经销商数据");
        }
    }

    @GetMapping("/page")
    public R<Page<T>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(required = false) Long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam Map<String, String> params) {
        long cur = page != null ? page : current;
        QueryWrapper<T> q = new QueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            q.and(
                    w -> {
                        for (String c : keywordColumns()) {
                            w.or().like(c, keyword);
                        }
                    });
        }
        applyFilters(q, params);
        applyScope(q);
        q.orderByDesc("id");
        return R.ok(mapper.selectPage(new Page<>(cur, size), q));
    }

    @GetMapping("/list")
    public R<List<T>> list(@RequestParam Map<String, String> params) {
        QueryWrapper<T> q = new QueryWrapper<>();
        applyFilters(q, params);
        applyScope(q);
        q.orderByAsc("id");
        return R.ok(mapper.selectList(q));
    }

    @GetMapping("/{id}")
    public R<T> get(@PathVariable Long id) {
        T entity = mapper.selectById(id);
        checkScope(entity);
        return R.ok(entity);
    }

    @PostMapping
    public R<T> create(@Valid @RequestBody T entity) {
        entity.setId(null);
        beforeSave(entity);
        forceScope(entity);
        mapper.insert(entity);
        return R.ok(entity);
    }

    @PutMapping("/{id}")
    public R<T> update(@PathVariable Long id, @Valid @RequestBody T entity) {
        checkScope(mapper.selectById(id));
        entity.setId(id);
        beforeSave(entity);
        forceScope(entity);
        mapper.updateById(entity);
        return R.ok(entity);
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        checkScope(mapper.selectById(id));
        mapper.deleteById(id);
        return R.ok();
    }

    protected void beforeSave(T entity) {}

    private static final List<String> RESERVED = Arrays.asList("current", "page", "size", "keyword");

    private void applyFilters(QueryWrapper<T> q, Map<String, String> params) {
        TableInfo info = TableInfoHelper.getTableInfo(entityClass);
        params.forEach(
                (k, v) -> {
                    if (RESERVED.contains(k) || StringUtils.isBlank(v)) {
                        return;
                    }
                    info.getFieldList().stream()
                            .filter(f -> f.getProperty().equals(k))
                            .findFirst()
                            .ifPresent(f -> q.eq(f.getColumn(), v));
                });
    }
}
