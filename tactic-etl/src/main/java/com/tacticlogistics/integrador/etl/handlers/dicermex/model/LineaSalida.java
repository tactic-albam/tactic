package com.tacticlogistics.integrador.etl.handlers.dicermex.model;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import com.tacticlogistics.integrador.etl.handlers.EtlEntidadBase;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(catalog = "dicermex", name = "lineas_salida")
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)

public class LineaSalida extends EtlEntidadBase {

	private static final long serialVersionUID = 1L;
	public static final String PREFIJO_ORDEN = "PREFIJO_ORDEN";
	public static final String NUMERO_ORDEN = "NUMERO_ORDEN";
	public static final String PRODUCTO_CODIGO = "PRODUCTO_CODIGO";
	public static final String CANTIDAD = "CANTIDAD";
	public static final String UNIDAD_CODIGO = "UNIDAD_CODIGO";
	public static final String BODEGA_ORIGEN_CODIGO = "BODEGA_ORIGEN_CODIGO";
	public static final String BODEGA_DESTINO_CODIGO = "BODEGA_DESTINO_CODIGO";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_linea_salida")
	private Long id;

	@Column(length = 3, nullable = false)
	@NotNull
	private String prefijoOrden;

	@Column(length = 20, nullable = false)
	@NotNull
	private String numeroOrden;

	@Column(length = 50, nullable = false)
	@NotNull
	private String productoCodigo;

	private int cantidad;

	@Column(length = 50, nullable = false)
	@NotNull
	private String unidadCodigo;

	@Column(length = 50, nullable = false)
	@NotNull
	private String bodegaOrigenCodigo;

	@Column(length = 50, nullable = false)
	@NotNull
	private String bodegaDestinoCodigo;

	@Builder
	public LineaSalida(Long id, String prefijoOrden, String numeroOrden, String productoCodigo, int cantidad,
			String unidadCodigo, String bodegaOrigenCodigo, String bodegaDestinoCodigo, Long idArchivo, String linea,
			LocalDateTime fechaCreacion, String usuarioCreacion) {
		super(idArchivo, linea, fechaCreacion, usuarioCreacion);
		this.id = id;
		this.prefijoOrden = prefijoOrden;
		this.numeroOrden = numeroOrden;
		this.productoCodigo = productoCodigo;
		this.cantidad = cantidad;
		this.unidadCodigo = unidadCodigo;
		this.bodegaOrigenCodigo = bodegaOrigenCodigo;
		this.bodegaDestinoCodigo = bodegaDestinoCodigo;
	}
}
