package com.taobao.tddl.optimizer.core.expression.bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.taobao.tddl.common.exception.NotSupportException;
import com.taobao.tddl.common.jdbc.ParameterContext;
import com.taobao.tddl.common.utils.TStringUtil;
import com.taobao.tddl.optimizer.core.ASTNodeFactory;
import com.taobao.tddl.optimizer.core.PlanVisitor;
import com.taobao.tddl.optimizer.core.expression.ExtraFunctionManager;
import com.taobao.tddl.optimizer.core.expression.IBindVal;
import com.taobao.tddl.optimizer.core.expression.IExtraFunction;
import com.taobao.tddl.optimizer.core.expression.IFunction;
import com.taobao.tddl.optimizer.core.expression.ISelectable;

/**
 * 采取代理模式，将function处理转移到执行器中进行处理，比如处理分库的count/max等
 * 
 * @since 5.1.0
 */
public class Function<RT extends IFunction> implements IFunction<RT> {

    public static final Object[] emptyArgs       = new Object[0];
    // count
    protected String             functionName;
    protected List               args            = new ArrayList();
    protected String             alias;
    protected boolean            distinct        = false;
    protected String             tablename;

    // count(id)
    protected String             columnName;
    protected IExtraFunction     extraFunction;
    private boolean              isNot;
    private boolean              needDistinctArg = false;

    public String getFunctionName() {
        return functionName;
    }

    public IFunction setFunctionName(String functionName) {
        this.functionName = functionName;
        return this;
    }

    public RT setDataType(DATA_TYPE dataType) {
        throw new NotSupportException();
    }

    public String getAlias() {
        return alias;
    }

    public String getTableName() {
        return tablename;
    }

    public String getColumnName() {
        return columnName;
    }

    public RT setAlias(String alias) {
        this.alias = alias;
        return (RT) this;
    }

    public RT setTableName(String tableName) {
        this.tablename = tableName;
        return (RT) this;
    }

    public RT setColumnName(String columnName) {
        this.columnName = columnName;
        return (RT) this;
    }

    public boolean isSameName(ISelectable select) {
        String cn1 = this.getColumnName();
        if (TStringUtil.isNotEmpty(this.getAlias())) {
            cn1 = this.getAlias();
        }

        String cn2 = select.getColumnName();
        if (TStringUtil.isNotEmpty(select.getAlias())) {
            cn2 = select.getAlias();
        }

        return TStringUtil.equals(cn1, cn2);
    }

    public String getFullName() {
        return this.getColumnName();
    }

    public boolean isDistinct() {
        return distinct;
    }

    public boolean isNot() {
        return isNot;
    }

    public RT setDistinct(boolean distinct) {
        this.distinct = distinct;
        return (RT) this;
    }

    public RT setIsNot(boolean isNot) {
        this.isNot = isNot;
        return (RT) this;
    }

    public List getArgs() {
        return args;
    }

    public RT setArgs(List args) {
        this.args = args;
        return (RT) this;
    }

    public boolean isNeedDistinctArg() {
        return needDistinctArg;
    }

    public RT setNeedDistinctArg(boolean b) {
        this.needDistinctArg = b;
        return (RT) this;
    }

    public RT copy() {
        IFunction funcNew = ASTNodeFactory.getInstance().createFunction();
        funcNew.setFunctionName(getFunctionName())
            .setAlias(this.getAlias())
            .setTableName(this.getTableName())
            .setColumnName(this.getColumnName())
            .setDistinct(this.isDistinct())
            .setIsNot(this.isNot());

        if (getArgs() != null) {
            List<Object> argsNew = new ArrayList(getArgs().size());
            for (Object arg : getArgs()) {
                if (arg instanceof ISelectable) {
                    argsNew.add(((ISelectable) arg).copy());
                } else if (arg instanceof IBindVal) {
                    argsNew.add(((IBindVal) arg));
                } else {
                    argsNew.add(arg);
                }

            }
            funcNew.setArgs(argsNew);
        }
        return (RT) funcNew;
    }

    /**
     * 复制一下function属性
     */
    protected void copy(IFunction funcNew) {
        funcNew.setFunctionName(getFunctionName())
            .setAlias(this.getAlias())
            .setTableName(this.getTableName())
            .setColumnName(this.getColumnName())
            .setDistinct(this.isDistinct())
            .setIsNot(this.isNot());

        if (getArgs() != null) {
            List<Object> argsNew = new ArrayList(getArgs().size());
            for (Object arg : getArgs()) {
                if (arg instanceof ISelectable) {
                    argsNew.add(((ISelectable) arg).copy());
                } else if (arg instanceof IBindVal) {
                    argsNew.add(((IBindVal) arg));
                } else {
                    argsNew.add(arg);
                }

            }
            funcNew.setArgs(argsNew);
        }
    }

    public void accept(PlanVisitor visitor) {
        visitor.visit(this);
    }

    public int compareTo(Object o) {
        throw new NotSupportException();
    }

    public RT assignment(Map<Integer, ParameterContext> parameterSettings) {
        if (getArgs() != null) {
            List<Object> argsNew = getArgs();
            int index = 0;
            for (Object arg : getArgs()) {
                if (arg instanceof ISelectable) {
                    argsNew.set(index, ((ISelectable) arg).assignment(parameterSettings));
                } else if (arg instanceof IBindVal) {
                    argsNew.set(index, ((IBindVal) arg).assignment(parameterSettings));
                } else {
                    argsNew.set(index, arg);
                }
                index++;
            }
            this.setArgs(argsNew);
        }

        return (RT) this;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.getColumnName());
        if (this.getAlias() != null) {
            builder.append(" as ").append(this.getAlias());
        }
        return builder.toString();
    }

    public IExtraFunction getExtraFunction() {
        if (extraFunction == null) {
            extraFunction = ExtraFunctionManager.getExtraFunction(getFunctionName());
            extraFunction.setFunction(this);// 不可能为null
        }
        return extraFunction;
    }

    public RT setExtraFunction(IExtraFunction function) {
        this.extraFunction = function;
        return (RT) this;
    }

    public FunctionType getFunctionType() {
        return getExtraFunction().getFunctionType();
    }

    public DATA_TYPE getDataType() {
        return getExtraFunction().getReturnType();
    }
}
