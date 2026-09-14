package com.dms.workshop.service;

import com.dms.workshop.entity.WorkOrder;
import com.dms.workshop.entity.WorkOrderLabor;
import com.dms.workshop.entity.WorkOrderPart;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 纯逻辑：工单状态机 + 金额/税额计算，便于单测。 */
public final class WorkOrderCalculator {
    private WorkOrderCalculator() {}

    public static final String DRAFT = "DRAFT";
    public static final String CHECKED_IN = "CHECKED_IN";
    public static final String DIAGNOSED = "DIAGNOSED";
    public static final String QUOTED = "QUOTED";
    public static final String APPROVED = "APPROVED";
    public static final String DISPATCHED = "DISPATCHED";
    public static final String IN_REPAIR = "IN_REPAIR";
    public static final String QC_PENDING = "QC_PENDING";
    public static final String QC_PASSED = "QC_PASSED";
    public static final String QC_FAILED = "QC_FAILED";
    public static final String SETTLED = "SETTLED";
    public static final String DELIVERED = "DELIVERED";
    public static final String CLOSED = "CLOSED";
    public static final String CANCELLED = "CANCELLED";

    private static final Map<String, Set<String>> TRANSITIONS = new HashMap<>();

    static {
        TRANSITIONS.put(DRAFT, set(CHECKED_IN, CANCELLED));
        TRANSITIONS.put(CHECKED_IN, set(DIAGNOSED, CANCELLED));
        TRANSITIONS.put(DIAGNOSED, set(QUOTED, CANCELLED));
        TRANSITIONS.put(QUOTED, set(APPROVED, CANCELLED));
        TRANSITIONS.put(APPROVED, set(DISPATCHED, CANCELLED));
        TRANSITIONS.put(DISPATCHED, set(IN_REPAIR));
        TRANSITIONS.put(IN_REPAIR, set(QC_PENDING));
        TRANSITIONS.put(QC_PENDING, set(QC_PASSED, QC_FAILED));
        TRANSITIONS.put(QC_FAILED, set(IN_REPAIR));
        TRANSITIONS.put(QC_PASSED, set(SETTLED));
        TRANSITIONS.put(SETTLED, set(DELIVERED));
        TRANSITIONS.put(DELIVERED, set(CLOSED));
    }

    private static Set<String> set(String... s) {
        return new HashSet<>(Arrays.asList(s));
    }

    public static boolean canTransit(String from, String to) {
        Set<String> allowed = TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    /** 重算工单金额：保修项计入 warrantyAmount，不向客户收；税额 = 客户应付/(1+税率)*税率。 */
    public static void compute(WorkOrder o, List<WorkOrderLabor> labors, List<WorkOrderPart> parts) {
        BigDecimal labor = BigDecimal.ZERO;
        BigDecimal partsSum = BigDecimal.ZERO;
        BigDecimal warranty = BigDecimal.ZERO;
        for (WorkOrderLabor l : labors) {
            BigDecimal amt =
                    l.getAmount() != null
                            ? l.getAmount()
                            : l.getHours().multiply(l.getRate()).setScale(2, RoundingMode.HALF_UP);
            labor = labor.add(amt);
            if (Boolean.TRUE.equals(l.getIsWarranty())) {
                warranty = warranty.add(amt);
            }
        }
        for (WorkOrderPart p : parts) {
            BigDecimal amt =
                    p.getAmount() != null
                            ? p.getAmount()
                            : new BigDecimal(p.getQty())
                                    .multiply(p.getUnitPrice())
                                    .setScale(2, RoundingMode.HALF_UP);
            partsSum = partsSum.add(amt);
            if (Boolean.TRUE.equals(p.getIsWarranty())) {
                warranty = warranty.add(amt);
            }
        }
        BigDecimal total = labor.add(partsSum).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount =
                o.getDiscountAmount() == null ? BigDecimal.ZERO : o.getDiscountAmount();
        BigDecimal payable = total.subtract(warranty).subtract(discount).max(BigDecimal.ZERO);
        BigDecimal taxRate = new BigDecimal("0.13");
        BigDecimal tax =
                payable
                        .divide(BigDecimal.ONE.add(taxRate), 10, RoundingMode.HALF_UP)
                        .multiply(taxRate)
                        .setScale(2, RoundingMode.HALF_UP);
        o.setLaborAmount(labor);
        o.setPartsAmount(partsSum);
        o.setTotalAmount(total);
        o.setWarrantyAmount(warranty);
        o.setCustomerPayable(payable);
        o.setTaxAmount(tax);
    }
}
