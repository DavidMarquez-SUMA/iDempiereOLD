/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2008 e-Evolution,SC. All Rights Reserved.               *
 * Contributor(s): Victor Perez www.e-evolution.com                           *
 *****************************************************************************/
package org.eevolution.process;


import java.math.BigDecimal;
import java.sql.PreparedStatement;//Agregado por P.S.
import java.sql.ResultSet;//Agregado por P.S.
import java.sql.Timestamp;
import java.util.logging.Level;

import org.compiere.model.MBankAccount;
import org.compiere.model.MPayment;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.report.FinReportPeriod;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;//AGREGADO POR P.S.
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
 
/**
 *  Bank Transfer. Generate two Payments entry
 *  
 *  For Bank Transfer From Bank Account "A" 
 *                 
 *	@author victor.perez@e-evoltuion.com
 *	
 **/
public class BankTransfer extends SvrProcess
{
	private String 		p_DocumentNo= "";				// Document No
	private String 		p_Description= "";				// Description
	private int 		p_C_BPartner_ID = 0;   			// Business Partner to be used as bridge
	private int			p_C_Currency_ID = 0;			// Payment Currency
	private int 		p_C_ConversionType_ID = 0;		// Payment Conversion Type
	private int			p_C_Charge_ID = 0;				// Charge to be used as bridge

	private BigDecimal 	p_Amount = Env.ZERO;  			// Amount to be transfered between the accounts
	private int 		p_From_C_BankAccount_ID = 0;	// Bank Account From
	private int 		p_To_C_BankAccount_ID= 0;		// Bank Account To
	private Timestamp	p_StatementDate = null;  		// Date Statement
	private Timestamp	p_DateAcct = null;  			// Date Account
	private int         p_AD_Org_ID = 0;
	private int         m_created = 0;
/*******************AGREGADO POR P.S. PARA TRANSFERENCIA ENTRE BANCOS**********************/
	private int         p_C_DocType_ID = 0;
	private int         p_C_DocTypeTarget_ID = 0;
/*******************FIN AGREGADO POR P.S. PARA TRANSFERENCIA ENTRE BANCOS*******************/	
	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (name.equals("From_C_BankAccount_ID"))
				p_From_C_BankAccount_ID = para[i].getParameterAsInt();
			else if (name.equals("To_C_BankAccount_ID"))
				p_To_C_BankAccount_ID = para[i].getParameterAsInt();
			else if (name.equals("C_BPartner_ID"))
				p_C_BPartner_ID = para[i].getParameterAsInt();
			else if (name.equals("C_Currency_ID"))
				p_C_Currency_ID = para[i].getParameterAsInt();
			else if (name.equals("C_ConversionType_ID"))
				p_C_ConversionType_ID = para[i].getParameterAsInt();
			else if (name.equals("C_Charge_ID"))
				p_C_Charge_ID = para[i].getParameterAsInt();
			else if (name.equals("DocumentNo"))
				p_DocumentNo = (String)para[i].getParameter();
			else if (name.equals("Amount"))
				p_Amount = ((BigDecimal)para[i].getParameter());	
			else if (name.equals("Description"))
				p_Description = (String)para[i].getParameter();
			else if (name.equals("StatementDate"))
				p_StatementDate = (Timestamp)para[i].getParameter();
			else if (name.equals("DateAcct"))
				p_DateAcct = (Timestamp)para[i].getParameter();
			else if (name.equals("AD_Org_ID"))
				p_AD_Org_ID = para[i].getParameterAsInt();
/*******************AGREGADO POR P.S. PARA TRANSFERENCIA ENTRE BANCOS**********************/			
			else if (name.equals("C_DocType_ID"))
				p_C_DocType_ID = para[i].getParameterAsInt();
			else if (name.equals("C_DocTypeTarget_ID"))
				p_C_DocTypeTarget_ID = para[i].getParameterAsInt();
/*******************AGREGADO POR P.S. PARA TRANSFERENCIA ENTRE BANCOS**********************/
			else
				log.log(Level.SEVERE, "prepare - Unknown Parameter: " + name);
		}
	}	//	prepare

	/**
	 *  Perform process.
	 *  @return Message (translated text)
	 *  @throws Exception if not successful
	 */
	protected String doIt() throws Exception
	{
/*******************************MODIFICADO POR P.S. PARA TRANSFERENCIA ENTRE BANCOS******************************************************/		
		if (log.isLoggable(Level.INFO)) log.info("From Bank="+p_From_C_BankAccount_ID+" - To Bank="+p_To_C_BankAccount_ID
				+ " - C_BPartner_ID="+p_C_BPartner_ID+"- C_Charge_ID= "+p_C_Charge_ID+" - Amount="+p_Amount+" - DocumentNo="+p_DocumentNo
				+ " - Description="+p_Description+ " - Statement Date="+p_StatementDate+
				" - Date Account="+p_DateAcct+ " - C_DocType_ID="+p_C_DocType_ID+" - C_DocType_ID_C="+p_C_DocTypeTarget_ID);
/*******************************FIN MODIFICADO POR P.S. PARA TRANSFERENCIA ENTRE BANCOS******************************************************/
		if (p_To_C_BankAccount_ID == 0 || p_From_C_BankAccount_ID == 0)
			throw new AdempiereUserError (Msg.parseTranslation(getCtx(), "@FillMandatory@: @To_C_BankAccount_ID@, @From_C_BankAccount_ID@"));

		if (p_To_C_BankAccount_ID == p_From_C_BankAccount_ID)
			throw new AdempiereUserError (Msg.getMsg(getCtx(), "BankFromToMustDiffer"));
		
		if (p_C_BPartner_ID == 0)
			throw new AdempiereUserError (Msg.parseTranslation(getCtx(), "@FillMandatory@ @C_BPartner_ID@"));
		
		if (p_C_Currency_ID == 0)
			throw new AdempiereUserError (Msg.parseTranslation(getCtx(), "@FillMandatory@ @C_Currency_ID@"));
		
		if (p_C_Charge_ID == 0)
			throw new AdempiereUserError (Msg.parseTranslation(getCtx(), "@FillMandatory@ @C_Charge_ID@"));
	
		if (p_Amount.signum() == 0)
			throw new AdempiereUserError (Msg.parseTranslation(getCtx(), "@FillMandatory@ @Amount@"));

		if (p_AD_Org_ID == 0)
			throw new AdempiereUserError (Msg.parseTranslation(getCtx(), "@FillMandatory@ @AD_Org_ID@"));
/*******************************AGREGADO POR P.S. PARA TRANSFERENCIA ENTRE BANCOS******************************************************/	
		if (p_C_DocType_ID == 0)
			throw new AdempiereUserError (Msg.parseTranslation(getCtx(), "@FillMandatory@ @C_DocType_ID@"));
		
		if (p_C_DocTypeTarget_ID == 0)
			throw new AdempiereUserError (Msg.parseTranslation(getCtx(), "@FillMandatory@ @C_DocTypeTarget_ID@"));
/*******************************FIN AGREGADO POR P.S. PARA TRANSFERENCIA ENTRE BANCOS******************************************************/			
		//	Login Date
		if (p_StatementDate == null)
			p_StatementDate = Env.getContextAsDate(getCtx(), "#Date");
		if (p_StatementDate == null)
			p_StatementDate = new Timestamp(System.currentTimeMillis());			

		if (p_DateAcct == null)
			p_DateAcct = p_StatementDate;

		generateBankTransfer();
		return "@Created@ = " + m_created;
	}	//	doIt
	

	/**
	 * Generate BankTransfer()
	 *
	 */
	private void generateBankTransfer()
	{

		MBankAccount mBankFrom = new MBankAccount(getCtx(),p_From_C_BankAccount_ID, get_TrxName());
		MBankAccount mBankTo = new MBankAccount(getCtx(),p_To_C_BankAccount_ID, get_TrxName());
		
		MPayment paymentBankFrom = new MPayment(getCtx(), 0 ,  get_TrxName());
/**********************************************************************************************************************/		
/*******sql Agregado por P.S. para obtener el IsSOTrx y el IsCash dependiendo del Tipo de Documento Efectivo o No Para El PAGO*****/
/**********************************************************************************************************************/		
		String sql = "SELECT dc.isSOTrx, CASE WHEN dc.Name LIKE'%EFECTIVO%' THEN 1 ELSE 0 END AS iscash "
				+ "FROM C_Doctype dc "
				+ "WHERE dc.IsActive='Y' AND C_DocType_ID=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		boolean isventaPago=false;
		boolean iscashPago=false;
		try
		{
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, p_C_DocType_ID);
			rs = pstmt.executeQuery();
			rs.next();
			isventaPago = rs.getBoolean(1);
			iscashPago = rs.getBoolean(2);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
/**********************************************************************************************************************/		
/*******sql Agregado por P.S. para obtener el IsSOTrx y el IsCash dependiendo del Tipo de Documento Efectivo o No Para el COBRO*****/
/**********************************************************************************************************************/		
		String sql2 = "SELECT dc.isSOTrx, CASE WHEN dc.Name LIKE'%EFECTIVO%' THEN 1 ELSE 0 END AS iscash "
				+ "FROM C_Doctype dc "
				+ "WHERE dc.IsActive='Y' AND C_DocType_ID=?";
		PreparedStatement pstmt2 = null;
		ResultSet rs2 = null;
		boolean isventaCobro=false;
		boolean iscashCobro=false;
		try
		{
			pstmt2 = DB.prepareStatement(sql2, null);
			pstmt2.setInt(1, p_C_DocTypeTarget_ID);
			rs2 = pstmt2.executeQuery();
			rs2.next();
			isventaCobro = rs2.getBoolean(1);
			iscashCobro = rs2.getBoolean(2);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, sql2, e);
		}
		finally
		{
			DB.close(rs2, pstmt2);
			rs = null; pstmt2 = null;
		}
/**********************************************************************************************************************/		
/*******FIN DEL Agregado por P.S. para obtener el IsSOTrx y el IsCash dependiendo del Tipo de Documento Efectivo o No *****/
/**********************************************************************************************************************/		
		paymentBankFrom.setC_BankAccount_ID(mBankFrom.getC_BankAccount_ID());
		paymentBankFrom.setAD_Org_ID(p_AD_Org_ID);
		if (!Util.isEmpty(p_DocumentNo, true))
			paymentBankFrom.setDocumentNo(p_DocumentNo);
		paymentBankFrom.setDateAcct(p_DateAcct);
		paymentBankFrom.setDateTrx(p_StatementDate);
		paymentBankFrom.setTenderType(MPayment.TENDERTYPE_DirectDeposit);
		paymentBankFrom.setDescription(p_Description);
		paymentBankFrom.setC_BPartner_ID (p_C_BPartner_ID);
		paymentBankFrom.setC_Currency_ID(p_C_Currency_ID);
		if (p_C_ConversionType_ID > 0)
			paymentBankFrom.setC_ConversionType_ID(p_C_ConversionType_ID);	
		paymentBankFrom.setPayAmt(p_Amount);
		paymentBankFrom.setOverUnderAmt(Env.ZERO);
		//paymentBankFrom.setC_DocType_ID(false); Original
		paymentBankFrom.setC_DocType_ID(p_C_DocType_ID);//Cambiado por P.S.
	//	paymentBankFrom.(iscashPago);//Creado por P.S.
		paymentBankFrom.setIsSOTrx(isventaPago);//Creado por P.S.
		paymentBankFrom.setIsReceipt(isventaPago);//Creado por P.S.
		paymentBankFrom.setC_Charge_ID(p_C_Charge_ID);
		paymentBankFrom.saveEx();
		if(!paymentBankFrom.processIt(MPayment.DOCACTION_Complete)) {
			log.warning("Payment Process Failed: " + paymentBankFrom + " - " + paymentBankFrom.getProcessMsg());
			throw new IllegalStateException("Payment Process Failed: " + paymentBankFrom + " - " + paymentBankFrom.getProcessMsg());
		}
		paymentBankFrom.saveEx();
		addBufferLog(paymentBankFrom.getC_Payment_ID(), paymentBankFrom.getDateTrx(),
				null, paymentBankFrom.getC_DocType().getName() + " " + paymentBankFrom.getDocumentNo(),
				MPayment.Table_ID, paymentBankFrom.getC_Payment_ID());
		m_created++;

		MPayment paymentBankTo = new MPayment(getCtx(), 0 ,  get_TrxName());
		paymentBankTo.setC_BankAccount_ID(mBankTo.getC_BankAccount_ID());
		paymentBankTo.setAD_Org_ID(p_AD_Org_ID);
		if (!Util.isEmpty(p_DocumentNo, true))
			paymentBankTo.setDocumentNo(p_DocumentNo);
		paymentBankTo.setDateAcct(p_DateAcct);
		paymentBankTo.setDateTrx(p_StatementDate);
		paymentBankTo.setTenderType(MPayment.TENDERTYPE_DirectDeposit);
		paymentBankTo.setDescription(p_Description);
		paymentBankTo.setC_BPartner_ID (p_C_BPartner_ID);
		paymentBankTo.setC_Currency_ID(p_C_Currency_ID);
		if (p_C_ConversionType_ID > 0)
			paymentBankTo.setC_ConversionType_ID(p_C_ConversionType_ID);	
		paymentBankTo.setPayAmt(p_Amount);
		paymentBankTo.setOverUnderAmt(Env.ZERO);
		//paymentBankTo.setC_DocType_ID(true);
		paymentBankTo.setC_DocType_ID(p_C_DocTypeTarget_ID, true);//Cambiado por P.S.
		paymentBankTo.setIsCash(iscashCobro);//Creado por P.S.
		paymentBankTo.setIsSOTrx(isventaCobro);//Creado por P.S.
		paymentBankTo.setIsReceipt(isventaCobro);//Creado por P.S.
		paymentBankTo.setC_Charge_ID(p_C_Charge_ID);
		paymentBankTo.saveEx();
		if (!paymentBankTo.processIt(MPayment.DOCACTION_Complete)) {
			log.warning("Payment Process Failed: " + paymentBankTo + " - " + paymentBankTo.getProcessMsg());
			throw new IllegalStateException("Payment Process Failed: " + paymentBankTo + " - " + paymentBankTo.getProcessMsg());
		}
		//paymentBankTo.saveEx();
		//java.lang.System.out.println(paymentBankTo.getDateTrx()+" ------ "+paymentBankTo.getC_DocType().getName() + " " + paymentBankTo.getDocumentNo());
		addBufferLog(paymentBankTo.getC_Payment_ID(), paymentBankTo.getDateTrx(),
				null, paymentBankTo.getC_DocType().getName() + " " + paymentBankTo.getDocumentNo(),
				MPayment.Table_ID, paymentBankTo.getC_Payment_ID());
		m_created++;
		return;

	}  //  generateBankTransfer
	
}	//	BankTransfer
