/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SL 
 * All portions are Copyright (C) 2001-2006 Openbravo SL 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
*/
package com.xmarts.Nomina.ad_actionButton;

import org.openbravo.dal.service.OBDal;
import com.sysfore.payroll.data.SPMSlabline;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.xmlEngine.XmlDocument;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;


public class NL_Slab extends HttpSecureAppServlet {
  

  public void init (ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost (HttpServletRequest request, HttpServletResponse response) throws IOException,ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    if (vars.commandIn("DEFAULT")) {
        String strslablineid = vars.getStringParameter("inpspmSlablineId"); 
        //System.out.println("Id del bloque ID: " + strslablineid);
        String stramt = vars.getStringParameter("inpamt");  
        //System.out.println("amt "+stramt);
        String strpor = vars.getStringParameter("inpemNlPorcentaje");
        //System.out.println("Porcentaje "+strpor);
      try {
        printPage(response, vars, strslablineid,stramt,strpor);
      } catch (ServletException ex) {
        pageErrorCallOut(response);
      }
    } else pageError(response);
  }

   void printPage(HttpServletResponse response, VariablesSecureApp vars,String strslablineid,String stramt,String strpor) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) log4j.debug("Output: dataSheet");
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
    
    double monto = new java.lang.Double(stramt);
    double porcen = new java.lang.Double(strpor);

    System.out.println("Monto "+monto);
    System.out.println("Porcentaje "+porcen);

    StringBuffer resultado = new StringBuffer();
    resultado.append("var calloutName='NL_Slab';\n\n");
    resultado.append("var respuesta = new Array("); 
    int cero = 0;

    if(monto > 0){
      //System.out.println("entro");
       resultado.append("new Array(\"inpemNlPorcentaje\", \"" + cero + "\")\n");   
       resultado.append(");\n");
       System.out.println("paso");  
    } 
    
    xmlDocument.setParameter("array", resultado.toString());
    xmlDocument.setParameter("frameName", "frameAplicacion");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }
}
