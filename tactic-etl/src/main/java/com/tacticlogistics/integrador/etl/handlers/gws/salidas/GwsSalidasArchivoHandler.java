package com.tacticlogistics.integrador.etl.handlers.gws.salidas;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import com.tacticlogistics.ClienteCodigoType;
import com.tacticlogistics.integrador.etl.decorators.CamposSplitterDecorator;
import com.tacticlogistics.integrador.etl.decorators.CheckArchivoVacioDecorator;
import com.tacticlogistics.integrador.etl.decorators.CheckNumeroDeColumnasDecorator;
import com.tacticlogistics.integrador.etl.decorators.CheckRegistrosDuplicadosDecorator;
import com.tacticlogistics.integrador.etl.decorators.CheckRestriccionesDeCamposDecorator;
import com.tacticlogistics.integrador.etl.decorators.Decorator;
import com.tacticlogistics.integrador.etl.decorators.LineasSplitterDecorator;
import com.tacticlogistics.integrador.etl.decorators.MayusculasDecorator;
import com.tacticlogistics.integrador.etl.handlers.ArchivoHandler;
import com.tacticlogistics.integrador.etl.handlers.heinz.salidas.model.SalidaCadena;
import com.tacticlogistics.integrador.etl.handlers.heinz.salidas.model.SalidaCadenaRepository;
import com.tacticlogistics.integrador.etl.readers.ExcelWorkSheetReaderGamma;
import com.tacticlogistics.integrador.etl.readers.Reader;

@Component
public class GwsSalidasArchivoHandler extends ArchivoHandler<SalidaCadena,Long> {
	private static final String CODIGO_TIPO_ARCHIVO = "HEINZ_SALIDAS_CADENAS";

	private static final String WORKSHEET_NAME = "CADENAS";

	@Autowired
	private ExcelWorkSheetReaderGamma reader;

	@Autowired
	private SalidaCadenaRepository repository;

	// ----------------------------------------------------------------------------------------------------------------
	//
	// ----------------------------------------------------------------------------------------------------------------
	@Override
	protected Reader getReader() {
		if (this.reader.getWorkSheetName() == null) {
			this.reader.setWorkSheetName(WORKSHEET_NAME);
		}
		return reader;
	}

	@Override
	protected String getCodigoTipoArchivo() {
		return CODIGO_TIPO_ARCHIVO;
	}

	@Override
	protected Path getCliente() {
		Path result = Paths.get(ClienteCodigoType.HEINZ.toString());
		return result;
	}

	@Override
	protected Path getSubDirectorioRelativo() {
		Path result = Paths.get("SALIDAS\\CADENAS");
		return result;
	}

	@Override
	protected Pattern getFileNamePattern() {
		return PATTERN_XLS;
	}
	
	@Override
	protected Decorator<SalidaCadena> getTransformador() {
		// @formatter:off
		return new MapEntidadSalidaDecorator(
				new CheckRegistrosDuplicadosDecorator<SalidaCadena>(
					new CheckRestriccionesDeCamposDecorator<SalidaCadena>(
						new CamposSplitterDecorator<SalidaCadena>(
							new CheckNumeroDeColumnasDecorator<SalidaCadena>(
								new CheckArchivoVacioDecorator<SalidaCadena>(
									new LineasSplitterDecorator<SalidaCadena>(
										new MayusculasDecorator<SalidaCadena>(
		))))))));
		// @formatter:on
	}

	@Override
	protected JpaRepository<SalidaCadena, Long> getRepository() {
		return repository;
	}
}
