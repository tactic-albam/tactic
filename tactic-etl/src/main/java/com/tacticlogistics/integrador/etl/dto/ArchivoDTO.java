package com.tacticlogistics.integrador.etl.dto;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.tacticlogistics.integrador.etl.model.Archivo;
import com.tacticlogistics.integrador.etl.model.TipoArchivo;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Builder
@Data
public class ArchivoDTO<T> {
	@NonNull
	private Path pathArchivo;
	@NonNull
	private TipoArchivo tipoArchivo;
	@NonNull
	private Archivo archivo;

	private String datos;

	private List<RegistroDTO<T>> registros;

	public static class ArchivoDTOBuilder<T> {
		private List<RegistroDTO<T>> registros = new ArrayList<>();
	}
}
