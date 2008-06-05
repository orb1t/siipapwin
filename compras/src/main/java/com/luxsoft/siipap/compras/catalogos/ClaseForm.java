package com.luxsoft.siipap.compras.catalogos;

import javax.swing.JComponent;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.luxsoft.siipap.swing.binding.Binder;
import com.luxsoft.siipap.swing.form2.AbstractForm;
import com.luxsoft.siipap.swing.form2.IFormModel;

/**
 * Forma para el mantenimiento de lineas
 * 
 * @author Ruben Cancino
 *
 */
public class ClaseForm extends AbstractForm{

	public ClaseForm(IFormModel model) {
		super(model);
		setTitle("Mantenimiento de Clase");
	}

	@Override
	protected JComponent buildFormPanel() {
		final FormLayout layout=new FormLayout(
				"p,2dlu,max(100;p), 3dlu,p,2dlu,f:max(100dlu;p):g"
				,"");
		final DefaultFormBuilder builder=new DefaultFormBuilder(layout);
		builder.append("Nombre",getControl("nombre"),5);		
		return builder.getPanel();
	}
	
	
	
	@Override
	protected JComponent createCustomComponent(String property) {
		if(property.equals("nombre"))
			return Binder.createMayusculasTextField(model.getComponentModel(property));
		else
			return null;
	}
	

}
