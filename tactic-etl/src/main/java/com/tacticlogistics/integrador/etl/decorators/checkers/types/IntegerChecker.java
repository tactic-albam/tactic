package com.tacticlogistics.integrador.etl.decorators.checkers.types;

import java.util.List;

import com.tacticlogistics.integrador.etl.decorators.checkers.CampoChecker;
import com.tacticlogistics.integrador.etl.model.Campo;
import com.tacticlogistics.integrador.etl.model.TipoDatoType;

import lombok.val;

public class IntegerChecker extends DataTypeChecker<Long> {
	@Override
	protected Long parse(Campo campo, String valor) {
		try {
			val result = Long.parseLong(valor);
			return result;
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	protected String getEjemplosValidos(Campo campo) {
		StringBuilder sb = new StringBuilder();
		sb.append(123456);
		sb.append(",");
		sb.append(-123456);

		return sb.toString();
	}

	@Override
	protected TipoDatoType getTipoDato() {
		return TipoDatoType.INTEGER;
	}

	@Override
	protected List<CampoChecker<Long>> getCheckers() {
		val result = super.getCheckers();
		result.add(new IntegerMinChecker());
		result.add(new IntegerMaxChecker());
		return result;
	}
}
