package com.luxsoft.siipap.swing.utils;

import java.text.MessageFormat;
import java.util.logging.Level;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;



import com.luxsoft.siipap.swing.Application;



public class MessageUtils {
	
	public static void showError(final String title,Throwable tx){		
		//JXErrorDialog.showDialog(getMainFrame(), title, tx);
		tx.printStackTrace();
		ErrorInfo info=new ErrorInfo(
				title
				,tx.getMessage()
				,tx.getLocalizedMessage()
				,"GRAVE"
				,tx
				,Level.SEVERE
				,null);
		
		JXErrorPane.showDialog(tx);		
		
		//tx.printStackTrace();
		//showError(tx.getMessage());
	}
	
	public static void showError(String msg){		
		JOptionPane.showMessageDialog(
				getMainFrame(), msg,"Error de ejecución",JOptionPane.ERROR_MESSAGE);
	}
	public static void showMessage(String msg,String titulo){
		JOptionPane.showMessageDialog(getMainFrame(), msg,titulo,JOptionPane.INFORMATION_MESSAGE);
	}
	
	protected static JFrame getMainFrame(){
		if(Application.isLoaded()){
			return Application.instance().getMainFrame();
		}else
			return null;
	}
	
	public static boolean showConfirmationMessage(final String msg,final String title){
		int res=JOptionPane.showConfirmDialog(getMainFrame()
				,msg
				,title
				,JOptionPane.YES_NO_OPTION
				,JOptionPane.QUESTION_MESSAGE);
		if(res==JOptionPane.YES_OPTION){
			return true;
		}
		return false;
	}

}
