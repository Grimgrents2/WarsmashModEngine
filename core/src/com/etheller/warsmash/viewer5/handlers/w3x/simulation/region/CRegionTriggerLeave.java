package com.etheller.warsmash.viewer5.handlers.w3x.simulation.region;

import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.TriggerExecutionScope;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.scope.trigger.TriggerBooleanExpression;
import com.etheller.warsmash.parsers.jass.scope.CommonTriggerExecutionScope;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;

public class CRegionTriggerLeave {
	private final GlobalScope globalScope;
	private final Trigger trigger;
	private final TriggerBooleanExpression filter;

	public CRegionTriggerLeave(final GlobalScope globalScope, final Trigger trigger,
			final TriggerBooleanExpression filter) {
		this.globalScope = globalScope;
		this.trigger = trigger;
		this.filter = filter;
	}

	public void fire(final CUnit unit, final CRegion region) {
		if (this.filter.evaluate(this.globalScope,
				CommonTriggerExecutionScope.filterScope(TriggerExecutionScope.EMPTY, unit))) {
			final CommonTriggerExecutionScope eventScope = CommonTriggerExecutionScope
					.unitLeaveRegionScope(TriggerExecutionScope.EMPTY, unit, region);
			if (this.trigger.evaluate(this.globalScope, eventScope)) {
				this.trigger.execute(this.globalScope, eventScope);
			}
		}
	}
}
