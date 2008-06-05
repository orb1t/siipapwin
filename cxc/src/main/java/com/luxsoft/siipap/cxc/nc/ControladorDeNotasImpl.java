package com.luxsoft.siipap.cxc.nc;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.util.Assert;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;

import com.luxsoft.siipap.cxc.consultas.ConsultaUtils;
import com.luxsoft.siipap.cxc.domain.Cliente;
import com.luxsoft.siipap.cxc.domain.ClienteCredito;
import com.luxsoft.siipap.cxc.domain.NotaDeCredito;
import com.luxsoft.siipap.cxc.domain.PagoConNota;
import com.luxsoft.siipap.cxc.domain.PagoConOtros;
import com.luxsoft.siipap.cxc.domain.PagoM;
import com.luxsoft.siipap.cxc.managers.CXCManager;
import com.luxsoft.siipap.cxc.managers.NotasManager;
import com.luxsoft.siipap.cxc.managers.PagosManager;
import com.luxsoft.siipap.cxc.model.CXCFiltros;
import com.luxsoft.siipap.cxc.model.NotasUtils;
import com.luxsoft.siipap.cxc.model.PagosFactory;
import com.luxsoft.siipap.cxc.model.PagosFactoryImpl;
import com.luxsoft.siipap.cxc.model2.DefaultPagoFormModelImpl;
import com.luxsoft.siipap.cxc.model2.NotaDeCreditoFormModel;
import com.luxsoft.siipap.cxc.model2.NotaDeCreditoFormModelImp;
import com.luxsoft.siipap.cxc.model2.NotaDeDevolucionFormModel;
import com.luxsoft.siipap.cxc.model2.NotaDeDevolucionFormModelImpl;
import com.luxsoft.siipap.cxc.model2.PagoConOtrosModelImpl;
import com.luxsoft.siipap.cxc.model2.PagoFormModel;
import com.luxsoft.siipap.cxc.model2.SolicitudDeNotaDeCargo2;
import com.luxsoft.siipap.cxc.pagos.PagoConNotaForm;
import com.luxsoft.siipap.cxc.pagos.PagoConOtrosForm;
import com.luxsoft.siipap.cxc.pagos.PagosAutomaticosNotasDeCargoForm;
import com.luxsoft.siipap.cxc.selectores.Selectores;
import com.luxsoft.siipap.services.ServiceLocator;
import com.luxsoft.siipap.swing.form2.BasicBindingFactory;
import com.luxsoft.siipap.swing.utils.MessageUtils;
import com.luxsoft.siipap.ventas.domain.Devolucion;
import com.luxsoft.siipap.ventas.domain.Venta;
import com.luxsoft.siipap.ventas.managers.VentasManager;

/**
 * Implementación de ControladorDeNotas
 * 
 * @author Ruben Cancino
 *
 */
public class ControladorDeNotasImpl  implements ControladorDeNotas{
	
	private NotasManager manager;
	private CXCManager cxcManager;
	private VentasManager ventasManager;
	private PagosFactory pagosFactory;
	private PagosManager pagosManager;
	
	protected Logger logger=Logger.getLogger(getClass());
	
	public void mostrarNota(final NotaDeCredito nota) {
		//getManager().
		getManager().refresh(nota);
		final RONotaDeCreditoFormModel model=new RONotaDeCreditoFormModel(nota);
		final RONotaDeCreditoForm form=new RONotaDeCreditoForm(model);
		form.setBindingFactory(new BasicBindingFactory());
		form.open();
	}
	
	public List<ClienteCredito> buscarClientesACredito() {
		return getCxcManager().buscarClientesACredito();
	}
	
	/**
	 * Genera las notas de descuento anticipado para las ventas seleccionadas
	 * 
	 */
	public List<NotaDeCredito> generarNotasDeDescuentoPorAnticipado(final Cliente c,final List<Venta> ventas,final  Date fecha) {
		NotasUtils.validarMismoCliente(c, ventas);
		final NCDescuentoFormModel model=new NCDescuentoFormModel(c,ventas,true);
		model.setFecha(fecha);
		final NCDescuentoForm form=new NCDescuentoForm(model);
		form.open();
		if(!form.hasBeenCanceled()){
			final List<NotaDeCredito> notas=model.procesar();
			for(NotaDeCredito nota:notas){
				getManager().salvarNotaCre(nota);
			}
			
			return notas;
		}		
		return null;
	}

	public void mostrarVenta(Venta v) {
		ConsultaUtils.mostrarVenta(v);
		
	}

	public Venta refrescar(Venta v) {
		getVentasManager().refresh(v);
		return v;
	}
	
	public void refrescar(NotaDeCredito nota) {
		getManager().refresh(nota);
	}

	/**
	 * Elimina la nota de credito/cargo
	 */
	public void eliminarNota(NotaDeCredito nota) {
		try {
			getManager().eliminar(nota);
		} catch (Exception e) {
			MessageUtils.showError("Error al tratar de eliminar la nota\n"+e.getLocalizedMessage());
			e.printStackTrace();
		}
		
		
	}
	
	/**
	 * 
	 * Busca la lista de devoluciones del cliente sin nota de credito
	 * 
	 * @param c
	 * @return
	 */
	public List<Devolucion> buscarDevolucionesDisponibles(final Cliente c){
		return getManager().buscarDevolucionesDisponibles(c);
	}
	
	/**
	 * Genera una nota de credito por devolucion
	 * 
	 * @param c
	 * @return
	 */
	public List<NotaDeCredito> aplicarNotaPorDevolucion(final Cliente c){
		Assert.notNull(c,"El cliente no puede ser nulo");
		List<Devolucion> devos=buscarDevolucionesDisponibles(c);
		if(devos.isEmpty()){
			MessageUtils.showMessage("Este cliente no tiene devoluciones pendientes", "Devoluciones");
		}else{
			final Devolucion d=Selectores.seleccionarDevolucion(c, GlazedLists.eventList(devos));
			if(d!=null){
				
				final NotaDeDevolucionFormModel model=new NotaDeDevolucionFormModelImpl(d);
				final NCDevoForm form=new NCDevoForm(model);
				form.open();
				if(!form.hasBeenCanceled()){					
					final List<NotaDeCredito> notas=model.procesar();
					if(notas!=null && !notas.isEmpty()){
						getManager().salvarNCDevoCRE(notas);						
						imprimirNotasDeCredito(notas);
					}					
				}
				
			}
		}
		return null;
	}
	
	/**
	 * Generar una o mas notas de credito por bonificacion
	 * 
	 * @param c
	 * @param ventas
	 * @return
	 */
	public List<NotaDeCredito> aplicarNotaPorBonificacion(final Cliente c,final List<Venta> ventas){
		
		final NotaDeCredito nota=NotasUtils.getNotaDeCreditoBonificacionCRE(c);
		final NotaDeCreditoFormModel model=new NotaDeCreditoFormModelImp(nota,ventas);
		final NotaDeCreditoForm form=new NotaDeCreditoForm(model);
		form.open();
		if(!form.hasBeenCanceled()){
			final List<NotaDeCredito> notas=model.procesar();
			if(!notas.isEmpty()){				
				getManager().salvarNotasCre(notas);
				imprimirNotasDeCredito(notas);
				return notas;
			}
		}		
		return null;
	}
	
	/**
	 * Genera una o mas nota de credito por descuento financiero
	 * 
	 * @param c
	 * @param ventas
	 * @return
	 */
	public List<NotaDeCredito> aplicarNotaPorDescuentoFinanciero(final Cliente c,final List<Venta> ventas){
		final NotaDeCredito nota=NotasUtils.getNotaDeCreditoDescFinancieroCRE(c);
		final NotaDeCreditoFormModel model=new NotaDeCreditoFormModelImp(nota,ventas);
		final NotaDeCreditoForm form=new NotaDeCreditoForm(model);
		form.open();
		if(!form.hasBeenCanceled()){
			final List<NotaDeCredito> notas=model.procesar();
			if(!notas.isEmpty()){
				getManager().salvarNotasCre(notas);
				imprimirNotasDeCredito(notas);
				return notas;
			}
		}		
		return null;
	}
	
	
	
	/**
	 * Genera una nota de cargo para la solicitud desada
	 * 
	 */
	public List<NotaDeCredito> generarNotasDeCargo(final Cliente cliente,final List<Venta> ventas) {
		
		if(ventas.isEmpty()) return null;		
		
		final SolicitudDeNotaDeCargo2 solicitud=new SolicitudDeNotaDeCargo2(cliente,ventas);
		final NotaDeCargoForm form=new NotaDeCargoForm(solicitud);
		form.open();
		if(!form.hasBeenCanceled()){
			List<NotaDeCredito> notas=NotasUtils.generarNotasDeCargo(solicitud);
			for(NotaDeCredito n:notas){
				getManager().salvarNotaCre(n);
			}
			return notas;
		}
		return null;
	}
	
	
	/**
	 * Manda imprimir una serie de notas de credito
	 * 
	 * @param notas
	 */
	public void imprimirNotasDeCredito(final List<NotaDeCredito> notas){		
		ImpresionDeNotas.imprimir(notas);
		/**
		final FormaDeImpresion f2=new FormaDeImpresion(notas);
		f2.open();
		if(!f2.hasBeenCanceled()){
			
			final Runtime r=Runtime.getRuntime();
			
			for(NotaDeCredito nota: notas){
				try {
					getManager().imprimirNotaDeDescuento(nota);
					//Runtime r=Runtime.getRuntime();
					//Process proc=r.exec(new String[]{"IMPRNOTA.BAT"});
					//int res=proc.waitFor();
					//System.out.println("Resultado de impresion: "+nota.getId()+" Res: "+res);
					
					//ServiceLocator.getNotasManager().imprimirNotaDeDescuento(n);
					Process p=r.exec(new String[]{"IMPRNOTA.BAT"});
					int res=p.waitFor();
					
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
		}
		**/		
	}
	
	
	

	/*** Colaboradores ***/

	public NotasManager getManager() {
		return manager;
	}
	public void setManager(NotasManager manager) {
		this.manager = manager;
	}	

	public CXCManager getCxcManager() {
		return cxcManager;
	}
	public void setCxcManager(CXCManager cxcManager) {
		this.cxcManager = cxcManager;
	}

	public VentasManager getVentasManager() {
		return ventasManager;
	}
	public void setVentasManager(VentasManager ventasManager) {
		this.ventasManager = ventasManager;
	}

	public PagoConNota aplicarPagoConNota(Cliente c, List<NotaDeCredito> cargos) {
		
		//Obtener una lista de las posibles notas de credito para el pago para que el usuario seleccione una
		final EventList<NotaDeCredito> notas=GlazedLists.eventList(getManager().buscarNotasDeCreditoDisponibles(c));		
		if(notas.isEmpty()){
			MessageUtils.showMessage(MessageFormat.format("El cliente {0} ({1})\n No tiene notas disponibles para usar como forma de pago"
					, c.getNombre(),c.getClave()), "Notas disponibles");
			return null;
		}
				
		final NotaDeCredito origen=Selectores.seleccionarNotaDeCredito(c, notas);
		//Procedemos con el pago
		if(origen!=null){
			final PagosFactory fac=new PagosFactoryImpl();
			final PagoConNota pago=fac.crearPagoDeCargoConNota(origen, cargos);
			final PagoFormModel model=new DefaultPagoFormModelImpl(pago,false);	
			final PagoConNotaForm form=new PagoConNotaForm(model);
			form.open();
			if(!form.hasBeenCanceled()){
				ServiceLocator.getPagosManager().salvarGrupoDePagos(pago);
				return pago;
			}
		}	
		
		return null;
	}

	public PagoConOtros aplicarPagoConOtros(final Cliente c, final List<NotaDeCredito> cargos,final List<PagoM> disponibles) {
		CXCFiltros.filtrarCargosConSaldo(cargos);		
		final PagoM origen=Selectores.seleccionarPagoM(c, disponibles);
		if(origen!=null){
			final PagoConOtros pago=getPagosFactory().crearPagoDeCargo(origen, cargos);
			pago.setOrigen(origen);
			pago.setCliente(origen.getCliente());
			final PagoConOtrosModelImpl model=new PagoConOtrosModelImpl(false,pago);
			final PagoConOtrosForm form=new PagoConOtrosForm(model);
			form.open();
			if(!form.hasBeenCanceled()){
				getPagosManager().salvarGrupoDePagos(pago);
				return pago;
			}
		}else{
			MessageUtils.showMessage("La selección de cargos no califica para el pago", "Pagos");
			
		}
		return null;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.luxsoft.siipap.cxc.nc.ControladorDeNotas#cancelarNotaDeCargo(com.luxsoft.siipap.cxc.domain.NotaDeCredito)
	 */
	public void cancelarNotaDeCargo(final NotaDeCredito cargo){
		Assert.isTrue(cargo.getImporte().abs().amount().doubleValue()>0,"Esta cancelación es solo para notas de cargo");
		Assert.isTrue(cargo.getPagos()==0,"El cargo ya tiene pagos debe eliminarlos para cancelar");
		try {
			final NotaDeCredito credito=getManager().generarNotaDeCancelacion(cargo);
			getManager().refresh(credito);//Actualiza el disponible (Es dinamico)
			
			final List<NotaDeCredito> notas=new ArrayList<NotaDeCredito>();
			final List<NotaDeCredito> cargos=new ArrayList<NotaDeCredito>();			
			cargos.add(cargo);
			
			final PagosFactory fac=new PagosFactoryImpl();
			final PagoConNota pago=fac.crearPagoDeCargoConNota(credito, cargos);
			pago.setImporte(cargo.getTotalAsMoneda2());
			pago.setComentario("Cancelacion de nota/cargo: "+cargo.getNumero() );
			final PagoFormModel model=new DefaultPagoFormModelImpl(pago,false);			
			model.actualizarPagos();						
			ServiceLocator.getPagosManager().salvarGrupoDePagos(pago);
			
			notas.add(credito);
			imprimirNotasDeCredito(notas);
		} catch (Exception e) {
			MessageUtils.showError("Error cancelando cargo", e);
		}
	}
	
	/**
	 * Controla el proceso de aplicacion de pagos automaticos sobre un grupo de facturas
	 * 
	 * 
	 * @param ventas
	 */
	public boolean registrarPagoAutomatico(final List<NotaDeCredito> cargos){
		CXCFiltros.filtrarNotasDeCargoConSaldo(cargos);
		CXCFiltros.filtrarParaPagoDeNotasDeCargoAutomatico(cargos);
		if(!cargos.isEmpty() ){
			final PagosAutomaticosNotasDeCargoForm form=new PagosAutomaticosNotasDeCargoForm(cargos);
			form.open();
			if(!form.hasBeenCanceled()){
				final PagoM pago=getPagosFactory().crearPagoAutomaticoParaNotaDeCargo(cargos);
				getPagosManager().salvarGrupoDePagos(pago);
				form.dispose();
				return true;
			}else{
				form.dispose();
				return false;
			}
			
			
		}
		return false;
	}
	

	public List<PagoM> buscarDisponibles(Cliente c) {		
		return getPagosManager().buscarSaldosAFavor(c);
	}

	public PagosFactory getPagosFactory() {
		return pagosFactory;
	}

	public void setPagosFactory(PagosFactory pagosFactory) {
		this.pagosFactory = pagosFactory;
	}

	public PagosManager getPagosManager() {
		return pagosManager;
	}

	public void setPagosManager(PagosManager pagosManager) {
		this.pagosManager = pagosManager;
	}

 
	
}
