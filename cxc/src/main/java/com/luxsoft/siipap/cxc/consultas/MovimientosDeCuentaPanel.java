package com.luxsoft.siipap.cxc.consultas;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.jdesktop.swingx.JXFrame;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.jdesktop.swingx.JXFrame.StartPosition;
import org.springframework.orm.hibernate3.HibernateCallback;

import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.matchers.Matcher;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.uifextras.panel.HeaderPanel;
import com.luxsoft.siipap.cxc.domain.ClienteCredito;
import com.luxsoft.siipap.cxc.domain.NotaDeCredito;
import com.luxsoft.siipap.cxc.domain.NotasDeCreditoDet;
import com.luxsoft.siipap.cxc.domain.Pago;
import com.luxsoft.siipap.cxc.domain.PagoM;
import com.luxsoft.siipap.domain.CantidadMonetaria;
import com.luxsoft.siipap.services.ServiceLocator;
import com.luxsoft.siipap.swing.binding.Binder;
import com.luxsoft.siipap.swing.controls.SXAbstractDialog;
import com.luxsoft.siipap.swing.selectores.CheckBoxSelector;
import com.luxsoft.siipap.swing.utils.MessageUtils;
import com.luxsoft.siipap.swing.utils.SWExtUIManager;
import com.luxsoft.siipap.swing.utils.TaskUtils;
import com.luxsoft.siipap.utils.DateUtils;
import com.luxsoft.siipap.ventas.domain.Venta;

public class MovimientosDeCuentaPanel extends FilteredBrowserPanel<Movimiento>{
	
	private String cliente="";
	
	private Date corte=new Date();
	private Date desde=DateUtils.obtenerFecha("01/01/2008");
	
	private DateFormat df=new SimpleDateFormat("dd/MM/yyyy");
	
	public MovimientosDeCuentaPanel(){
		super(Movimiento.class);
		
 		
	}
	
	CheckBoxSelector<Movimiento> conSaldo;
	private JTextField saldoField=new JTextField(10);
	private JTextField saldoAFavor=new JTextField(10);
	private JTextField notaAFavor=new JTextField(10);
	
	public void init(){
		setProperties(PROPS);
		setLabels(NAMES);
		conSaldo=new CheckBoxSelector<Movimiento>(){
			@Override
			protected Matcher<Movimiento> getSelectMatcher(Object... obj) {
				return new Matcher<Movimiento>(){
					public boolean matches(Movimiento item) {
						return item.getSaldoCargo().doubleValue()>0;
					}
					
				};
			}
 			
 		};
 		
 		TextFilterator<Movimiento> fechaFilterator=new TextFilterator<Movimiento>(){
			public void getFilterStrings(List<String> baseList,Movimiento element) {
				baseList.add(df.format(element.getFecha()));
				
			}
 			
 		};

 		
 		installTextComponentMatcherEditor("Tipo","mov");
 		installTextComponentMatcherEditor("Docto","documento");
 		installTextComponentMatcherEditor("F.P","pagoRef");
 		installTextComponentMatcherEditor("Fecha", fechaFilterator, new JTextField(10));
	}
	
	private HeaderPanel header;
	

	@Override
	protected JComponent buildContent() {
		JPanel panel=new JPanel(new BorderLayout());
		JComponent parent=super.buildContent();
		panel.add(parent,BorderLayout.CENTER);		
		header=new HeaderPanel("Seleccione un cliente","");
		panel.add(header,BorderLayout.NORTH);
		
		JXTaskPaneContainer container=new JXTaskPaneContainer();
		//container.setPreferredSize(new Dimension(200,200));
		JXTaskPane filtrosPanel=new JXTaskPane();
		filtrosPanel.setTitle("Filtros");
		filtrosPanel.setSpecial(true);
		filtrosPanel.add(getFilterPanel());
		container.add(filtrosPanel);
		
		JXTaskPane accionesPanel=new JXTaskPane();
		accionesPanel.setTitle("Operaciones");
		accionesPanel.add(getLoadAction());
		container.add(accionesPanel);
		
		panel.add(container,BorderLayout.WEST);
		
		return panel;
	}
	
	public static String[] PROPS={
		"mov"
		,"id"
		,"documento"
		,"serie"
		,"tipo"
		,"cargoId"
		,"sucursal"
		,"fecha"
		,"revision"
		,"vencimiento"
		,"formaDePago"
		,"pagoRef"
		,"descuento"
		,"importe"
		,"aplicable"
		,"saldoAcumulado"
		,"saldoCargo"	
		,"saldoAFavor"		
		,"notaAFavor"
		,"aplicacionesAnteriores"
	};
	
	public static String[] NAMES={
		"MOV"
		,"Id"
		,"Docto"
		,"S"
		,"T"
		,"CargoId"
		,"Suc"
		,"Fecha"
		,"Rev"
		,"Vto"
		,"F.P."
		,"P.Ref"
		,"Dcto"
		,"Importe"
		,"Apl"
		,"Saldo Acu"
		,"Saldo Car"
		,"Saldo AFav"		
		,"NotaAFav"
		,"Aplic <2008"
	};
	
	public void actualizarAcumulado(){
		
		BigDecimal acu=BigDecimal.ZERO;
		for(int index=0;index<source.size();index++){			
			Movimiento element=(Movimiento)source.get(index);
			acu=acu.add(element.getImporte());
			element.setSaldoAcumulado(acu);
			source.set(index, element);
		}
	}
	
	public void actualizarSaldo(){
		for(int index=0;index<source.size();index++){
			Movimiento m=(Movimiento)source.get(index);
			if(m.getMov().equals("FAC")|| m.getMov().equals("CAR")){
				m.setSaldoCargo(getSaldo(m.getCargoId()));
				source.set(index, m);
			}
		}
		
	}
	
	@SuppressWarnings("unchecked")
	private BigDecimal getSaldo(final Long cargoId){
		Collection<Movimiento> res=CollectionUtils.select(source, new Predicate(){
			public boolean evaluate(Object object) {
				Movimiento m=(Movimiento)object;
				if(m.getCargoId()==null) return false;
				return m.getCargoId().equals(cargoId);
			}
			
		});
		BigDecimal saldo=BigDecimal.ZERO;
		for(Movimiento mm:res){
			saldo=saldo.add(mm.getImporte());
		}
		return saldo;
	}
	
	public void actualizarSaldos(){
		BigDecimal saldo=BigDecimal.ZERO;
		BigDecimal saldoAfavor=BigDecimal.ZERO;
		BigDecimal saldoNota=BigDecimal.ZERO;
		for(Object obj:source){
			Movimiento m=(Movimiento)obj;
			saldo=saldo.add(m.getSaldoCargo());
			saldoAfavor=saldoAfavor.add(m.getSaldoAFavor());
			saldoNota=saldoNota.add(m.getNotaAFavor());
		}
		NumberFormat nf=NumberFormat.getCurrencyInstance();
		this.saldoAFavor.setText(nf.format(saldoAfavor.doubleValue()));
		this.saldoField.setText(nf.format(saldo.doubleValue()));
		this.notaAFavor.setText(nf.format(saldoNota.doubleValue()));
	}
	
	private JFrame owner;
	
	
	public JFrame getOwner() {
		return owner;
	}

	public void setOwner(JFrame owner) {
		this.owner = owner;
	}

	public void seleccionarCliente(){
		final ValueModel clienteModel=new ValueHolder(null);
		final ValueModel fechaModel=new ValueHolder(new Date());
		
		final SXAbstractDialog dialog=new SXAbstractDialog(owner,"Buscar Movimientos"){
			@Override
			protected JComponent buildContent() {
				JPanel panel=new JPanel(new BorderLayout());
				FormLayout layout=new FormLayout("p,2dlu,150dlu","");
				DefaultFormBuilder builder=new DefaultFormBuilder(layout);
				builder.append("Cliente",Binder.createClientesCreditoBindingBox(clienteModel));
				builder.append("Corte",Binder.createDateComponent(fechaModel));
				panel.add(builder.getPanel(),BorderLayout.CENTER);
				panel.add(buildButtonBarWithOKCancel(),BorderLayout.SOUTH);
				return panel;
			}
			
		};
		
		dialog.open();
		if(!dialog.hasBeenCanceled() && (clienteModel.getValue()!=null)){
			ClienteCredito cliente=((ClienteCredito)clienteModel.getValue());
			this.cliente=cliente.getClave();
			this.corte=(Date)fechaModel.getValue();
			this.header.setTitle(cliente.getNombre());
			this.header.setDescription("Movimientos al: "+DateUtils.format(corte));
			
		}
	}
	
	public void load(){		
		seleccionarCliente();
		source.clear();
		getLoadAction().setEnabled(false);
		Loader loader=new Loader();
		TaskUtils.executeSwingWorker(loader);
	}
	
	@Override
	protected List<Movimiento> findData() {
		return null;
	}
	
	private class Loader extends SwingWorker<String, Movimiento> {

		@Override
		protected String doInBackground() throws Exception {
			ServiceLocator.getDaoSupport().getHibernateTemplate().execute(new HibernateCallback(){

				public Object doInHibernate(Session session) throws HibernateException, SQLException {
					
					// Agregamos todas las facturas
					String hql="from Venta v where v.cliente.clave=? and v.fecha between ? and ? and v.origen=\'CRE\'";
					ScrollableResults rs=session.createQuery(hql)
					.setParameter(0, cliente)
					.setParameter(1,desde ,Hibernate.DATE)
					.setParameter(2,corte ,Hibernate.DATE)
					.scroll();
					while(rs.next()){
						Venta v=(Venta)rs.get()[0];
						Movimiento m=new Movimiento(v);
						publish(m);
					}
					
					hql="from NotaDeCredito nc where nc.tipo=\'M\' and nc.clave=? and nc.fecha between ? and ?";				
					List<NotaDeCredito> ncargos=session.createQuery(hql)
					.setParameter(0, cliente)
					.setParameter(1,desde ,Hibernate.DATE)
					.setParameter(2,corte ,Hibernate.DATE)
					.list();
					for(NotaDeCredito nc:ncargos){					
						Movimiento m=new Movimiento(nc);
						publish(m);
					}
					
					hql="from NotasDeCreditoDet nc where nc.tipo!=\'M\' and nc.nota.clave=? and nc.fecha between ? and ? and nc.origen=\'CRE\'";
					rs=session.createQuery(hql)
					.setParameter(0, cliente)
					.setParameter(1,desde ,Hibernate.DATE)
					.setParameter(2,corte ,Hibernate.DATE)
					.scroll();
					while(rs.next()){
						NotasDeCreditoDet det=(NotasDeCreditoDet)rs.get()[0];
						Movimiento m=new Movimiento(det);
						publish(m);
					}
					
					//Agregamos los Pagos de Facturas				
					hql="from Pago p where p.venta.clave=? and p.venta.fecha between ? and ? and p.venta.origen=\'CRE\' and p.venta is not null";
					rs=session.createQuery(hql)
					.setParameter(0, cliente)
					.setParameter(1,desde ,Hibernate.DATE)
					.setParameter(2,corte ,Hibernate.DATE)
					.scroll();
					while(rs.next()){
						Pago pago=(Pago)rs.get()[0];
						Movimiento m=new Movimiento(pago);
						publish(m);
					}
					
					//Agregamos los Pagos de Notas de cargo				
					hql="from Pago p where p.nota.clave=? and p.nota.fecha between ? and ? and p.nota.origen=\'CRE\' and p.nota is not null";
					rs=session.createQuery(hql)
					.setParameter(0, cliente)
					.setParameter(1,desde ,Hibernate.DATE)
					.setParameter(2,corte ,Hibernate.DATE)
					.scroll();
					while(rs.next()){
						Pago pago=(Pago)rs.get()[0];
						Movimiento m=new Movimiento(pago);
						publish(m);
					}
					
					//Agregamos los disponibles
					hql="from PagoM p where p.clave=? and p.fecha between ? and ? ";
					rs=session.createQuery(hql)
					.setParameter(0, cliente)
					.setParameter(1,desde ,Hibernate.DATE)
					.setParameter(2,corte ,Hibernate.DATE)
					.scroll();
					while(rs.next()){
						PagoM pago=(PagoM)rs.get()[0];
						Movimiento m=new Movimiento(pago);
						CantidadMonetaria aplicadoAnt=CantidadMonetaria.pesos(0);
						for(Pago pa:pago.getPagos()){
							if(pa.getVenta()!=null){
								if(pa.getVenta().getFecha().compareTo(desde)<0){
									aplicadoAnt=aplicadoAnt.add(pa.getImporte());
								}
								if(pa.getNota()!=null){
									if(pa.getNota().getFecha().compareTo(desde)<0){
										aplicadoAnt=aplicadoAnt.add(pa.getImporte());
									}
								}
							}
							
						}
						if((aplicadoAnt.amount().doubleValue()>0)||(m.getSaldoAFavor().doubleValue()>0)){
							m.setAplicacionesAnteriores(aplicadoAnt.amount());
							publish(m);
						}	
						
					}					
					
					return null;
				}
				
			});
			return "OK";
		}

		@Override
		protected void process(List<Movimiento> chunks) {
			source.addAll(chunks);
		}
		
		@Override
		protected void done() {
			try{
				getLoadAction().setEnabled(true);
				grid.packAll();
				actualizarAcumulado();
				actualizarSaldo();
				actualizarSaldos();
			}catch (Exception e) {
				MessageUtils.showError("Error al cargar datos", e);
			}
		}
		
		
		
	}
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable(){
			public void run() {
				SWExtUIManager.setup();
				JXFrame app=new JXFrame("Estado de Cuenta",true);
				final MovimientosDeCuentaPanel control=new MovimientosDeCuentaPanel();
				app.setContentPane(control.getControl());
				app.setStartPosition(StartPosition.CenterInScreen);
				app.setExtendedState(JFrame.MAXIMIZED_BOTH);
				//app.pack();
				
				app.setVisible(true);
				
			}
		});
	}

	

}


