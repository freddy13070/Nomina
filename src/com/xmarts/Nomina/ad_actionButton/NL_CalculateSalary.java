/**
************************************************************************************
* Copyright (C) 2009-2010 Openbravo S.L.U.
* Licensed under the Openbravo Commercial License version 1.0
* You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
* or in the legal folder of this module distribution.
************************************************************************************

************************************************************************************
 *  Author			: Luis Alfredo Valencia Diaz
 *  Company			: Grupo Xmarts  
 *  Creation Date	: 09 de mayo del 2014
 ***********************************************************************************
*/

package com.xmarts.Nomina.ad_actionButton;

import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.model.ad.system.Client;

import com.sysfore.humanresource.data.SHREmpPunch;
import com.sysfore.payroll.data.SPMFormula;
import com.sysfore.payroll.data.SPMPayComp;
import com.sysfore.payroll.data.SPMPayHead;
import com.sysfore.payroll.data.SPMDeductions;
import com.sysfore.payroll.data.SPMSlab;
import com.sysfore.payroll.data.SPMPayCompAdd;
import com.sysfore.payroll.data.SPMPayCompDeduct;
import com.sysfore.payroll.data.SPMEmployerContrib;
import com.sysfore.payroll.data.SPMPayCompEmprcontrib;
import com.sysfore.payroll.data.SPMSlabline;

import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.math.BigDecimal;

import de.congrace.exp4j.Calculable;
import de.congrace.exp4j.ExpressionBuilder;

/**
 * Salary Components Calculation Process
 */
public class NL_CalculateSalary extends DalBaseProcess {

	@Override
	public void doExecute(ProcessBundle bundle) throws Exception {
		
		try {
			
			final String paycomp = (String) bundle.getParams().get(
					"SPM_Pay_Comp_ID");
			System.out.println(" ID :" + paycomp);
			bundle.setResult(calculate(paycomp));

		} catch (Exception e) {
			
			e.printStackTrace(System.err);
			final OBError msg = new OBError();
			msg.setType("Error"); // this will make the resulting message red
			msg.setMessage(e.getMessage());
			msg.setTitle(Utility.messageBD(new DalConnectionProvider(),
					"Error", OBContext.getOBContext().getLanguage()
							.getLanguage()));
			bundle.setResult(msg);
		}
	}

	OBError calculate(String strid) {
		String IDSUELDOBRUTO = "";
		String montoanual = "";
		String montomensual = "";
		OBError myMessage = new OBError();
		try{

		SPMPayComp paycomp = OBDal.getInstance().get(SPMPayComp.class, strid);
		Client client = paycomp.getClient();
		System.out.println("Client ID:" + client.getId());
		String document=paycomp.getDocumentNo();
		System.out.println("Document "+document);

		//En esta parte obtengo el sueldo bruto

		NLAddicionData[] sueldobruto = NLAddicionData.sueldo_bruto(new DalConnectionProvider(),paycomp.getId());

		if(sueldobruto.length == 0){
			myMessage.setMessage("Error");
		myMessage.setType("Error");
		myMessage.setTitle(Utility
				.messageBD(new DalConnectionProvider(), "Error no existe ningun adicciones de Sueldo Bruto", OBContext
						.getOBContext().getLanguage().getLanguage()));
		}

		for(int x=0;x < sueldobruto.length; x++){
			IDSUELDOBRUTO = sueldobruto[x].idhead;	
			montoanual =  sueldobruto[x].montoanual;	
			montomensual = sueldobruto[x].montomensual;	
			
		}
		double mntanual =  new java.lang.Double(montoanual.toString());

		/**********************Adicciones********************************/


		NLAddicionData[] adi = NLAddicionData.addic(new DalConnectionProvider(),paycomp.getId());

		for(int x=0;x < adi.length; x++){
			   System.out.println("**********************Adicciones********************************");
			   if(adi[x].tipopago.equals("S")){
			   	    //System.out.println("Entro en la adiccion Slab");
			   		SPMPayHead payhead = OBDal.getInstance().get(SPMPayHead.class,adi[x].idhead);

			   		String id=payhead.getId();
			   		//System.out.println("Id adiciones "+id);
			   		String idbloque =NLAddicionData.idslab(new DalConnectionProvider(),id);
			   		System.out.println("ID slad "+idbloque);
			   		if(idbloque.equals("")){
			   			System.out.println("Entro al if1");
			   			myMessage.setMessage("La addicion "+payhead.getName()+" no tiene un bloque asignado");
						myMessage.setType("Error");
						myMessage.setTitle(Utility.messageBD(new DalConnectionProvider(), "Error", OBContext.getOBContext().getLanguage().getLanguage()));

						return myMessage;
			   		}else{
			   			System.out.println("Entro else");

			   		SPMSlab slabhead = OBDal.getInstance().get(SPMSlab.class,payhead.getSPMSlab().getId());
			   		String id_slab=slabhead.getId();
			   		//System.out.println("id bloque "+id_slab);

			        NLSlabLinesData[] line = NLSlabLinesData.lines(new DalConnectionProvider(),id_slab);

			        System.out.println("Monto anual "+mntanual);

			   		for(int a=0;a< line.length;a++){
			   				//System.out.println("Id del componente de adiccion "+adi[x].addid);
			   			    SPMPayCompAdd spca = OBDal.getInstance().get(SPMPayCompAdd.class,adi[x].addid);
			   			    double fromamt = new java.lang.Double(line[a].froma.toString());
			   			    double toamt = new java.lang.Double(line[a].toa.toString());
			   			    double res = 0;
			   				System.out.println("DE CTC "+fromamt);
			   				System.out.println("A CTC "+toamt);
			   				System.out.println("Fijo "+line[a].fijo);
			   				if(line[a].fijo.equals("0")){
			   					 System.out.println("Entro al porcentaje");
			   					 if(mntanual >= fromamt && mntanual <= toamt){
			   					 	double porc = new java.lang.Double(line[a].porcen.toString());
			   					 	System.out.println("Entro al if del porcentaje "+porc);
			   					 	res = ((mntanual * porc) / 100);
			   					 	System.out.println("Resultado "+res);			   
			   					 	//System.out.println("ID del componentes "+spca.getId());
			   					 	BigDecimal amountyear = BigDecimal.valueOf(res);
			   					 	spca.setAmountperYear(amountyear);

			   					 }

			   				}else{
			   					System.out.println("Entro al fijo");
			   					if(mntanual >= fromamt && mntanual <= toamt){
			   						BigDecimal amountyear=new BigDecimal(line[a].fijo.toString());
			   						spca.setAmountperYear(amountyear);
			   					}		
			   				}
			   			}
			   		}
			   }
			   if(adi[x].tipopago.equals("FR")){
			   
			   	String amtyear ="";

				SPMPayHead payhead = OBDal.getInstance().get(SPMPayHead.class, adi[x].idhead);
				
				System.out.println("Formula="+payhead.getSPMFormula().getId());
				
				SPMFormula formula = OBDal.getInstance().get(SPMFormula.class, payhead.getSPMFormula().getId());
				
				System.out.println("Formula Key="+ formula.getFormula());
				
				String formulakey = formula.getFormula();
				
				StringTokenizer st = new StringTokenizer(formula.getFormula(), "+/*-%()");
				
				while (st.hasMoreElements()) {

					String strele = st.nextElement().toString();
					Pattern p = Pattern.compile("((-|\\+)?[0-9]+(\\.[0-9]+)?)+");
					Matcher m = p.matcher(strele);
					boolean flag = m.matches(); // TRUE
					
					if (!flag) {
						
					String payheadid = NLSlabLinesData.payheadid(new DalConnectionProvider(), client.getId(), strele.trim());
					
					amtyear = NLSlabLinesData.payheadamt(new DalConnectionProvider(), client.getId(), paycomp.getId(), payheadid);
					
					//System.out.println("Amount="+amtyear);
					
					formulakey = formulakey.replaceAll(strele, amtyear);
						
					
					}
					
				
				}
				
				System.out.println(" Updated formula key = "+ formulakey);
				
				
				Calculable calc = new ExpressionBuilder(formulakey).build();
	            
				double result=calc.calculate();
				
			
				System.out.println("Result="+ result);
				
				SPMPayCompAdd pca = OBDal.getInstance().get(SPMPayCompAdd.class, adi[x].addid);
					
				pca.setAmountperYear(new BigDecimal(result));
				OBDal.getInstance().save(pca);

			   }

			
		} //Aqui termina el for de la las adicciones

		//********************************Deducciones*****************************************/

		NLDeduccionesData[] ded = NLDeduccionesData.deducc(new DalConnectionProvider(),paycomp.getId());

		for(int x=0;x<ded.length;x++){
			System.out.println("********************************Deducciones*****************************************");
			//System.out.println("Tipo "+ded[x].tipopago);
			if(ded[x].tipopago.equals("S")){
			   	    System.out.println("Entro en la deducciones Slab "+ded[x].idhead);
			   		SPMDeductions deduction = OBDal.getInstance().get(SPMDeductions.class, ded[x].idhead);
			   		
			   		String id=deduction.getId();
			   		//System.out.println("Id deducciones  "+id);

			   		String idbloque = NLDeduccionesData.idslab(new DalConnectionProvider(),id);

			   		System.out.println("ID slad "+idbloque);


			   		if(idbloque.equals("")){
			   			System.out.println("Entro al if1");
			   			myMessage.setMessage("La deduccion "+deduction.getName()+" no tiene un bloque asignado");
						myMessage.setType("Error");
						myMessage.setTitle(Utility.messageBD(new DalConnectionProvider(), "Error", OBContext.getOBContext().getLanguage().getLanguage()));

						return myMessage;
			   		}else{
			   			System.out.println("Entro else");

			   		SPMSlab slabhead = OBDal.getInstance().get(SPMSlab.class,deduction.getSPMSlab().getId());

			   		String id_slab=slabhead.getId();

			   		

			   		boolean ISR = slabhead.isNlSubsidio();

			   		String isrs = NLISR.valorcampoisr(new DalConnectionProvider(),id_slab);		
					
					System.out.println("ISR boolean "+ISR);
					System.out.println("ISR string "+isrs);

			        if(isrs.charAt(0) == 'Y'){
					   			
			         NLISR[] line = NLISR.isr(new DalConnectionProvider(),id_slab);	

			         String id_subsidio = NLISR.valorsubsidio(new DalConnectionProvider(),id_slab);

			   		System.out.println("id subsidio "+id_subsidio);
			         	
			   		double mntquin =mntanual / 12;
			   		System.out.println("Monto quincenal "+mntquin);	
			   		for(int a=0;a< line.length;a++){
			   		double fromamt = new java.lang.Double(line[a].froma.toString());
			   		double toamt = new java.lang.Double(line[a].toa.toString());
			   			
			   		 	if(mntquin >= fromamt && mntquin <= toamt){
			   		 		System.out.println("Entro al ISR (limite inferior)"+fromamt);
			   		 		double excedente = mntquin - fromamt;
			   		 		System.out.println("Excedente del límite inferior "+excedente);
			   		 		double exc = new java.lang.Double(line[a].excen.toString());
			   		 		double excpor = exc / 100;
			   		 		System.out.println("% sobre excedente del límite inferior "+excpor);
			   		 		double impmarginal = excedente * excpor;
			   		 		System.out.println("Impuesto marginal "+impmarginal);
			   		 		double cuota = new java.lang.Double(line[a].cuota.toString());
			   		 		System.out.println("Cuota fija de impuesto "+cuota);
			   		 		double isrdet = cuota + impmarginal;
			   		 		System.out.println("ISR determinado "+isrdet);
			   		 		//Aqui empieza la comparacion con el subsidio
			   		 		NLSlabLinesData[] lin = NLSlabLinesData.lines(new DalConnectionProvider(),id_subsidio);
			   		 		for(int o=0;o<lin.length;o++){
			   		 				double fromam = new java.lang.Double(lin[o].froma.toString());
			   						double toam = new java.lang.Double(lin[o].toa.toString());
			   		 				if(mntquin >= fromam && mntquin <= toam){
			   		 					double fijo = new java.lang.Double(lin[o].fijo.toString());
			   		 					System.out.println("Subsidio para el empleo "+fijo);
			   		 					double isrretener = isrdet - fijo;
			   		 					System.out.println("ISR a retener "+isrretener);
			   							SPMPayCompDeduct spca = OBDal.getInstance().get(SPMPayCompDeduct.class,ded[x].dedid);
			   							BigDecimal amountyear = BigDecimal.valueOf(isrretener);
			   					 		spca.setAmountperYear(amountyear);
			   		 				}
			   		 		}
			   		 	}
			   		}//for	

			   		}else{

			        NLSlabLinesData[] line = NLSlabLinesData.lines(new DalConnectionProvider(),id_slab);

			        //System.out.println("Monto anual "+mntanual);

			   		for(int a=0;a< line.length;a++){
			   				//System.out.println("Id del componente de deduccion "+ded[x].dedid);
			   			    SPMPayCompDeduct spca = OBDal.getInstance().get(SPMPayCompDeduct.class,ded[x].dedid);
			   			    double fromamt = new java.lang.Double(line[a].froma.toString());
			   			    double toamt = new java.lang.Double(line[a].toa.toString());
			   			    double res = 0;
			   				System.out.println("DE CTC "+fromamt);
			   				System.out.println("A CTC "+toamt);
			   				System.out.println("Fijo "+line[a].fijo);
			   				if(line[a].fijo.equals("0")){
			   					 System.out.println("Entro al porcentaje");
			   					 if(mntanual >= fromamt && mntanual <= toamt){
			   					 	double porc = new java.lang.Double(line[a].porcen.toString());
			   					 	System.out.println("Entro al if del porcentaje "+porc);
			   					 	res = ((mntanual * porc) / 100);
			   					 	System.out.println("Resultado "+res);			   
			   					 	//System.out.println("ID del componentes "+spca.getId());
			   					 	BigDecimal amountyear = BigDecimal.valueOf(res);
			   					 	spca.setAmountperYear(amountyear);

			   					 }

			   				}else{
			   					System.out.println("Entro al fijo");
			   					if(mntanual >= fromamt && mntanual <= toamt){
			   						BigDecimal amountyear=new BigDecimal(line[a].fijo.toString());
			   						spca.setAmountperYear(amountyear);
			   					}		
			   				}

			   			}//for
			  		}
			  		}	
				}
			   if(ded[x].tipopago.equals("FR")){
			   
			   	String amtyear ="";

			   	SPMDeductions deduction = OBDal.getInstance().get(SPMDeductions.class, ded[x].idhead);
				
				System.out.println("Formula="+deduction.getSPMFormula().getId());
				
				SPMFormula formula = OBDal.getInstance().get(SPMFormula.class, deduction.getSPMFormula().getId());
				
				System.out.println("Formula Key="+ formula.getFormula());
				
				String formulakey = formula.getFormula();
				
				StringTokenizer st = new StringTokenizer(formula.getFormula(), "+/*-%()");
				
				while (st.hasMoreElements()) {

					String strele = st.nextElement().toString();
					Pattern p = Pattern.compile("((-|\\+)?[0-9]+(\\.[0-9]+)?)+");
					Matcher m = p.matcher(strele);
					boolean flag = m.matches(); // TRUE
					
					if (!flag) {
						
					String deductionsid = NLDeduccionesData.deductionsid(new DalConnectionProvider(), client.getId(), strele.trim());
					
					amtyear = NLDeduccionesData.deductionsamt(new DalConnectionProvider(), client.getId(), paycomp.getId(), deductionsid);
					
					System.out.println("Amount="+amtyear);
					
					if( amtyear == null) {
						
						String payheadid = NLSlabLinesData.payheadid(new DalConnectionProvider(), client.getId(), strele.trim());
						
						amtyear = NLSlabLinesData.payheadamt(new DalConnectionProvider(), client.getId(), paycomp.getId(), payheadid);
						
						System.out.println("Amount="+amtyear);
						
						
					}
					
					
					
					formulakey = formulakey.replaceAll(strele, amtyear);
						
					
					}
					
				
				}
				
				System.out.println(" Updated formula key = "+ formulakey);
				
				
				Calculable calc = new ExpressionBuilder(formulakey).build();
	            
				double result=calc.calculate();
				
			
				System.out.println("Result="+ result);
				
				SPMPayCompDeduct pcd = OBDal.getInstance().get(SPMPayCompDeduct.class, ded[x].dedid);
					
				pcd.setAmountperYear(new BigDecimal(result));
				OBDal.getInstance().save(pcd);
			}

		}//Aqui cierra el for de deducciones


		NLContribucionData[] contri = NLContribucionData.contrib(new DalConnectionProvider(),paycomp.getId());


		for(int x=0; x <contri.length ;x++){
				System.out.println("********************************Contribucion***************************************** "+contri[x].tipopago);

			if(contri[x].tipopago.equals("S")){
			   	    System.out.println("Entro en la contribucion Slab "+contri[x].idhead);
			   		SPMEmployerContrib deduction = OBDal.getInstance().get(SPMEmployerContrib.class, contri[x].idhead);
			   		SPMPayCompEmprcontrib spca = OBDal.getInstance().get(SPMPayCompEmprcontrib.class,contri[x].empid);
			   		String id=deduction.getId();
			   		//System.out.println("Id deducciones  "+id);

			   		String idbloque = NLContribucionData.idslab(new DalConnectionProvider(),id);

			   		System.out.println("ID slad "+idbloque);


			   		if(idbloque.equals("")){
			   			System.out.println("Entro al if1");
			   			myMessage.setMessage("La contribucion al empleado "+deduction.getCommercialName()+" no tiene un bloque asignado");
						myMessage.setType("Error");
						myMessage.setTitle(Utility.messageBD(new DalConnectionProvider(), "Error", OBContext.getOBContext().getLanguage().getLanguage()));

						return myMessage;
			   		}else{
			   			System.out.println("Entro else");

			   		SPMSlab slabhead = OBDal.getInstance().get(SPMSlab.class,deduction.getSPMSlab().getId());
			   		String id_slab=slabhead.getId();
			   		//.out.println("id bloque "+id_slab);

			        NLSlabLinesData[] line = NLSlabLinesData.lines(new DalConnectionProvider(),id_slab);

			        //System.out.println("Monto anual "+mntanual);

			   		for(int a=0;a< line.length;a++){
			   				//System.out.println("Id del componente de deduccion "+ded[x].dedid);
			   			    
			   			    double fromamt = new java.lang.Double(line[a].froma.toString());
			   			    double toamt = new java.lang.Double(line[a].toa.toString());
			   			    double res = 0;
			   				System.out.println("DE CTC "+fromamt);
			   				System.out.println("A CTC "+toamt);
			   				System.out.println("Fijo "+line[a].fijo);
			   				if(line[a].fijo.equals("0")){
			   					 System.out.println("Entro al porcentaje");
			   					 if(mntanual >= fromamt && mntanual <= toamt){
			   					 	double porc = new java.lang.Double(line[a].porcen.toString());
			   					 	System.out.println("Entro al if del porcentaje "+porc);
			   					 	res = ((mntanual * porc) / 100);
			   					 	System.out.println("Resultado "+res);			   
			   					 	//System.out.println("ID del componentes "+spca.getId());
			   					 	BigDecimal amountyear = BigDecimal.valueOf(res);
			   					 	spca.setAmountperYear(amountyear);

			   					 }

			   				}else{
			   					System.out.println("Entro al fijo");
			   					if(mntanual >= fromamt && mntanual <= toamt){
			   						BigDecimal amountyear=new BigDecimal(line[a].fijo.toString());
			   						spca.setAmountperYear(amountyear);
			   					}		
			   				}

			   			}
			  		}
				}

				if(contri[x].tipopago.equals("FR")) {
				
				SPMEmployerContrib contrib = OBDal.getInstance().get(SPMEmployerContrib.class, contri[x].idhead);
				
				System.out.println("Formula="+contrib.getSPMFormula().getId());
				
				SPMFormula formula = OBDal.getInstance().get(SPMFormula.class, contrib.getSPMFormula().getId());
				
				System.out.println("Formula Key="+ formula.getFormula());
				
				String formulakey = formula.getFormula();
				
				StringTokenizer st = new StringTokenizer(formula.getFormula(), "+/*-%()");
				
				while (st.hasMoreElements()) {

					String strele = st.nextElement().toString();
					Pattern p = Pattern.compile("((-|\\+)?[0-9]+(\\.[0-9]+)?)+");
					Matcher m = p.matcher(strele);
					boolean flag = m.matches(); // TRUE
					
					if (!flag) {
						
					String contribid = NLContribucionData.contribid(new DalConnectionProvider(), client.getId(), strele.trim());
					
					String amtyear = NLContribucionData.contribsamt(new DalConnectionProvider(), client.getId(), paycomp.getId(), contribid);
					
					System.out.println("Amount="+amtyear);
					
					if( amtyear == null) {
						
						String deductionsid = NLDeduccionesData.deductionsid(new DalConnectionProvider(), client.getId(), strele.trim());
						
						amtyear = NLDeduccionesData.deductionsamt(new DalConnectionProvider(), client.getId(), paycomp.getId(), deductionsid);
						
						System.out.println("Amount="+amtyear);
						
						if( amtyear == null) {
							
							String payheadid = NLSlabLinesData.payheadid(new DalConnectionProvider(), client.getId(), strele.trim());
							
							amtyear = NLSlabLinesData.payheadamt(new DalConnectionProvider(), client.getId(), paycomp.getId(), payheadid);
							
							System.out.println("Amount="+amtyear);
							
							
						}
						
						
					}
					
					formulakey = formulakey.replaceAll(strele, amtyear);
						
					
					}
					
				
				}
				
				System.out.println(" Updated formula key = "+ formulakey);
				
				
				Calculable calc = new ExpressionBuilder(formulakey).build();
	            
				double result=calc.calculate();
				
			
				System.out.println("Result="+ result);
				
				SPMPayCompEmprcontrib pcc = OBDal.getInstance().get(SPMPayCompEmprcontrib.class, contri[x].empid);
					
				pcc.setAmountperYear(new BigDecimal(result));
				OBDal.getInstance().save(pcc);
			}	


		}





		
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
		myMessage.setMessage("Success");
		myMessage.setType("Success");
		myMessage.setTitle(Utility
				.messageBD(new DalConnectionProvider(), "Success", OBContext
						.getOBContext().getLanguage().getLanguage()));
		
		return myMessage;
	}
	
	
	

}
