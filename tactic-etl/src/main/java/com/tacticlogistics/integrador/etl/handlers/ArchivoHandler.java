package com.tacticlogistics.integrador.etl.handlers;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.tacticlogistics.core.patterns.AbstractHandler;
import com.tacticlogistics.integrador.etl.decorators.Decorator;
import com.tacticlogistics.integrador.etl.decorators.ETLRuntimeException;
import com.tacticlogistics.integrador.etl.dto.ArchivoDTO;
import com.tacticlogistics.integrador.etl.model.TipoArchivo;
import com.tacticlogistics.integrador.etl.model.TipoArchivoRepository;
import com.tacticlogistics.integrador.etl.readers.Reader;
import com.tacticlogistics.integrador.etl.services.ArchivosService;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Getter(AccessLevel.PROTECTED)
@Slf4j
public abstract class ArchivoHandler<T, ID extends Serializable> extends AbstractHandler<ArchivoRequest> {
	protected static final Pattern PATTERN_TXT = Pattern.compile("(?i:.*\\.(txt|rpt|csv))");

	protected static final Pattern PATTERN_XLS = Pattern.compile("(?i:.*\\.(xlsx|xls))");

	protected static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

	protected DateTimeFormatter getDateTimeFormatter() {
		return dateTimeFormatter;
	}

	@Value("${etl.directorio.errores}")
	private String subDirectorioDeErrores;

	@Value("${etl.directorio.procesados}")
	private String subDirectorioDeProcesados;

	@Value("${etl.directorio.salidas}")
	private String subDirectorioDeSalidas;

	@Autowired
	private TipoArchivoRepository tipoArchivoRepository;

	@Autowired
	private ArchivosService archivosService;

	// ----------------------------------------------------------------------------------------------------------------
	//
	// ----------------------------------------------------------------------------------------------------------------
	abstract protected Reader getReader();

	abstract protected String getCodigoTipoArchivo();

	abstract protected Path getCliente();

	abstract protected Path getSubDirectorioRelativo();

	abstract protected Pattern getFileNamePattern();

	abstract protected Decorator<T> getTransformador();

	abstract protected JpaRepository<T, ID> getRepository();

	// ----------------------------------------------------------------------------------------------------------------
	// canHandleRequest
	// ----------------------------------------------------------------------------------------------------------------
	@Override
	protected boolean canHandleRequest(ArchivoRequest request) {
		if (request == null) {
			return false;
		}
		if (this.getReader() == null) {
			return false;
		}
		if (this.getSubDirectorioRelativo() == null) {
			return false;
		}
		if (this.getFileNamePattern() == null) {
			return false;
		}
		if (!checkCliente(request)) {
			return false;
		}
		if (!checkSubDirectorioRelativo(request)) {
			return false;
		}
		if (!checkNombreArchivo(request)) {
			return false;
		}

		return true;
	}

	protected boolean checkCliente(ArchivoRequest request) {
		return this.getCliente().equals(request.getCliente());
	}

	protected boolean checkSubDirectorioRelativo(ArchivoRequest request) {
		return this.getSubDirectorioRelativo().equals(request.getSubDirectorioRelativo());
	}

	protected boolean checkNombreArchivo(ArchivoRequest request) {
		// @formatter:off
		return this.getFileNamePattern()
				.matcher(request.getPathArchivo().getFileName().toString())
				.matches();
		// @formatter:on
	}

	// ----------------------------------------------------------------------------------------------------------------
	// handleRequest
	// ----------------------------------------------------------------------------------------------------------------
	@Override
	protected void handleRequest(ArchivoRequest request) {
		Assert.notNull(request);
		Path pathArchivo = request.getPathArchivo();
		Assert.notNull(pathArchivo);
		TipoArchivo tipoArchivo = tipoArchivoRepository.findOneByCodigo(getCodigoTipoArchivo());
		Assert.notNull(tipoArchivo);

		log.debug("Procesando el archivo {} de tipo {}", pathArchivo, tipoArchivo.getCodigo());

		boolean error = false;
		ArchivoDTO<T> archivoDTO = null;
		try {
			archivoDTO = archivosService.<T> crearArchivo(pathArchivo, tipoArchivo);
			archivoDTO = cargar(transformar(extraer(archivoDTO)));

			archivosService.marcarValido(archivoDTO.getArchivo());
		} catch (ETLRuntimeException e) {
			error = true;
			archivosService.marcarNoValidoPorEstructura(archivoDTO.getArchivo(), e.getErrores());
		} catch (IOException | RuntimeException e) {
			error = true;
			archivosService.marcarNoValidoPorExcepcion(archivoDTO.getArchivo(), e);
		} finally {
			if (!error) {
				backupProcesados(request);
			} else {
				backupErrores(request);
			}
		}
	}

	protected ArchivoDTO<T> extraer(ArchivoDTO<T> archivoDTO) throws IOException {
		String datos = this.getReader().read(archivoDTO.getPathArchivo());

		write(datos);

		archivoDTO.setDatos(datos);
		return archivoDTO;
	}

	public static void write(String datos) {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter("C:\\APPS\\datos.txt"))) {
			bw.write(datos);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected ArchivoDTO<T> transformar(ArchivoDTO<T> archivoDTO) {
		return this.getTransformador().transformar(archivoDTO);
	}

	@Transactional
	protected ArchivoDTO<T> cargar(ArchivoDTO<T> archivoDTO) {
		// @formatter:off
		val entidades = archivoDTO
				.getRegistros()
				.stream().map(a -> a.getEntidad())
				.collect(Collectors.toList());
		// @formatter:on

		getRepository().save(entidades);

		return archivoDTO;
	}

	// ----------------------------------------------------------------------------------------------------------------
	// BACKUP
	// ----------------------------------------------------------------------------------------------------------------
	protected void backupProcesados(ArchivoRequest request) {
		val archivo = request.getPathArchivo();
		String subDirectorio = this.getSubDirectorioDeProcesados();

		log.info("Realizando copia de seguridad del archivo {} en el subdirectorio {}", archivo, subDirectorio);

		try {
			backup(request, subDirectorio);
		} catch (IOException | RuntimeException e) {
			fatal(request, subDirectorio, e);
		}
	}

	protected void backupErrores(ArchivoRequest request) {
		val archivo = request.getPathArchivo();
		String subDirectorio = this.getSubDirectorioDeErrores();
		log.info("Realizando copia de seguridad del archivo {} en el subdirectorio {}", archivo, subDirectorio);

		try {
			backup(request, subDirectorio);
		} catch (IOException | RuntimeException e) {
			fatal(request, subDirectorio, e);
		}
	}

	protected void backup(ArchivoRequest request, String subDirectorioDestino) throws IOException {
		Path origen = request.getPathArchivo();
		Path destino = getPathDestino(request, subDirectorioDestino);

		crearDirectorioSiNoExiste(destino.getParent());
		Files.move(origen, destino);
	}

	protected Path getPathDestino(ArchivoRequest request, String subDirectorioDestino) {
		LocalDateTime fechaActualDelSistema = LocalDateTime.now();
		// @formatter:off
		Path result = request
				.getRoot()
				.getParent()
				.resolve(subDirectorioDestino)
				.resolve(this.getSubDirectorioRelativo())
				.resolve(this.getSubdirectorioBackup(request, fechaActualDelSistema))
				.resolve(this.getNombreArchivoBackup(request, fechaActualDelSistema));
		// @formatter:on

		return result;
	}

	protected Path getSubdirectorioBackup(ArchivoRequest request, LocalDateTime fechaActualDelSistema) {
		String value = fechaActualDelSistema.format(getDateTimeFormatter());
		Path result = Paths.get(value.substring(0, 6)).resolve(value.substring(0, 8));
		return result;
	}

	protected String getNombreArchivoBackup(ArchivoRequest request, LocalDateTime fechaActualDelSistema) {
		String value = fechaActualDelSistema.format(getDateTimeFormatter());
		String result = String.format("%s-%s", value, request.getPathArchivo().getFileName());
		return result;
	}

	protected void crearDirectorioSiNoExiste(final Path path) throws IOException {
		if (Files.notExists(path)) {
			log.info("Creando directorio {}", path);
			Files.createDirectories(path);
		}
	}

	// ----------------------------------------------------------------------------------------------------------------
	// FATAL
	// ----------------------------------------------------------------------------------------------------------------
	protected void fatal(ArchivoRequest request, String subDirectorio, Throwable t) {
		val archivo = request.getPathArchivo();
		val fechaActualDelSistema = LocalDateTime.now();

		log.error(
				"Ocurrio el siguiente error al intentar realizar la copia de seguridad del archivo {} en el directorio {}",
				archivo.toString(), subDirectorio, t.getClass().getName(), t);

		String value = fechaActualDelSistema.format(getDateTimeFormatter());
		String archivoError = String.format("%s-%s-%s.error", value, t.getClass().getName(), archivo.getFileName());

		try {
			Path pathError = archivo.resolveSibling(archivoError);
			Files.move(request.getPathArchivo(), pathError, REPLACE_EXISTING);
		} catch (IOException | RuntimeException e) {
			String mensaje = "Ocurrio el siguiente error al intentar renombrar el archivo {} al nombre {}";
			log.error(mensaje, archivo.toString(), archivoError, e);
		}
	}
}