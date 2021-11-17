/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
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
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 * Contributor(s): Layda Salas (globalqss)
 * Contributor(s): Carlos Ruiz (globalqss)
 *****************************************************************************/

package org.compiere.process;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;

import org.compiere.model.MAcctSchema;
import org.compiere.model.MClientInfo;
import org.compiere.model.MDiscountSchemaLine;
import org.compiere.model.MProduct;
import org.compiere.model.MProductPrice;
import org.compiere.model.MSequence;
import org.compiere.model.MUOMConversion;
import org.compiere.model.ProductCost;
import org.compiere.util.AdempiereSystemError;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.CLogger;
import org.compiere.util.CPreparedStatement;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.ValueNamePair;

/**
 * Create PriceList by copying purchase prices (M_Product_PO) 
 *		and applying product category discounts (M_CategoryDiscount)
 * 
 * @author Layda Salas (globalqss)
 * @version $Id: M_PriceList_Create,v 1.0 2005/10/09 22:19:00
 *          globalqss Exp $
 * @author Carlos Ruiz (globalqss)
 *         Make T_Selection tables permanent
 */
public class M_PriceList_CreateSM extends SvrProcess {

	/** The Record */
	private int p_PriceList_Version_ID = 0;

	private String p_DeleteOld;
	
	private int m_AD_PInstance_ID = 0; 
	private BigDecimal p_M_Pricelist_Version_Base_ID;
	/**
	 * Prepare - e.g., get Parameters.
	 */
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			//java.lang.System.out.println(i);
			if (para[i].getParameter() == null)
				;
			else if (name.equals("DeleteOld"))
				p_DeleteOld = (String) para[i].getParameter();
			else if (name.equals("M_Pricelist_Version_Base_ID"))
				p_M_Pricelist_Version_Base_ID = (BigDecimal) para[i].getParameter();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
		p_PriceList_Version_ID = getRecord_ID();
		m_AD_PInstance_ID = getAD_PInstance_ID();
	} //*prepare*/

	/**
	 * Process
	 * 
	 * @return message
	 * @throws Exception
	 */

	protected String doIt() throws Exception {
		StringBuilder sql = new StringBuilder();
		StringBuilder sqlupd = new StringBuilder();
		String sqldel;
		StringBuilder sqlins = new StringBuilder();
		int cntu = 0;
		int cntd = 0;
		int cnti = 0;
		@SuppressWarnings("unused")
		int totu = 0;
		@SuppressWarnings("unused")
		int toti = 0;
		@SuppressWarnings("unused")
		int totd = 0;
		int v_temp;
		int v_NextNo = 0;
		StringBuilder message = new StringBuilder();
		
		//	Make sure that we have only one active product
		//
		
		PreparedStatement stmtCurgen = null;
		ResultSet rsCurgen = null;
		PreparedStatement stmtDiscountLine = null;
		ResultSet rsDiscountLine = null;
		CPreparedStatement stmt = null;
		PreparedStatement pstmt = null;
		try {
			//int p_M_PriceList_Version_Base_ID = getRecord_ID();
			
			int seqproductpriceid = MSequence.get(getCtx(), "M_ProductPrice").get_ID();
			int currentUserID = Env.getAD_User_ID(getCtx());

			/*if (p_M_PriceList_Version_Base_ID==0) {

				return "NO";
			}else

			{*/
				sqlins = new StringBuilder("INSERT INTO M_ProductPrice ");					
				sqlins.append(" (M_ProductPrice_ID, M_ProductPrice_UU, M_PriceList_Version_ID, M_Product_ID,");
				sqlins.append(" AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,");
				sqlins.append(" PriceList, PriceStd, PriceLimit)");
				sqlins.append("SELECT ");
				sqlins.append("nextIdFunc(").append(seqproductpriceid).append(",'N')");
				sqlins.append(", generate_uuid(),");
				sqlins.append(p_PriceList_Version_ID);
				sqlins.append(", pp.M_Product_ID,");
				sqlins.append("pp.AD_Client_ID");
				sqlins.append(", ");
				sqlins.append("pp.AD_Org_ID");
				sqlins.append(", 'Y', SysDate,  ");
				sqlins.append(currentUserID);
				sqlins.append(", SysDate, ");
				sqlins.append(currentUserID);
				sqlins.append(" ,");
				// Price List
				sqlins.append("pp.PriceList");
				sqlins.append(",");
				sqlins.append("pp.PriceStd");
				sqlins.append(", ");
				sqlins.append("pp.PriceLimit");
				sqlins.append(" FROM M_ProductPrice pp");
				sqlins.append(" INNER JOIN M_PriceList_Version plv ON (pp.M_PriceList_Version_ID=plv.M_PriceList_Version_ID)");
				sqlins.append(" INNER JOIN M_PriceList pl ON (plv.M_PriceList_ID=pl.M_PriceList_ID)");
				sqlins.append(" WHERE pp.M_PriceList_Version_ID=");
				sqlins.append(p_M_Pricelist_Version_Base_ID);/*
				sqlins.append(" AND EXISTS (SELECT * FROM T_Selection s WHERE pp.M_Product_ID=s.T_Selection_ID"); 
					sqlins.append(" AND s.AD_PInstance_ID=").append(m_AD_PInstance_ID).append(")");*/
				sqlins.append(" AND	pp.IsActive='Y'");

	pstmt = DB.prepareStatement(sqlins.toString(),
			ResultSet.TYPE_SCROLL_INSENSITIVE,
			ResultSet.CONCUR_UPDATABLE, get_TrxName());/*
	pstmt.setTimestamp(1, rsDiscountLine.getTimestamp("ConversionDate"));
	pstmt.setTimestamp(2, rsDiscountLine.getTimestamp("ConversionDate"));
	pstmt.setTimestamp(3, rsDiscountLine.getTimestamp("ConversionDate"));*/

				cnti = pstmt.executeUpdate();
				if (cnti == -1)
					raiseError(
							" INSERT INTO T_Selection from existing PriceList",
							sqlins.toString());
				toti += cnti;
			
					message.append(", @Inserted@=").append(cnti);
					sqlupd = new StringBuilder("UPDATE	M_PriceList_Version p ");
					  sqlupd.append(" SET	IsActive  = 'N'");
					  sqlupd.append(" WHERE	 M_PriceList_Version_ID!=");
					  sqlupd.append(p_PriceList_Version_ID + " AND M_PriceList_ID = (");
					  sqlupd.append(" SELECT M_PriceList_ID FROM M_PriceList_Version pr WHERE ");
					  sqlupd.append(" M_PriceList_Version_ID = ");
					  sqlupd.append(p_PriceList_Version_ID).append(")");
		cntu = DB.executeUpdate(sqlupd.toString(), get_TrxName());
		if (cntu == -1)
			raiseError("Update  M_ProductPrice ", sqlupd.toString());

			//}
		} catch (SQLException e) {
			throw e;
		} finally {
			DB.close(rsCurgen, stmtCurgen);
			rsCurgen = null; stmtCurgen = null;
			DB.close(rsDiscountLine, stmtDiscountLine);
			rsDiscountLine = null; stmtDiscountLine = null;
			DB.close(stmt);
			stmt = null;
			DB.close(pstmt);
			pstmt = null;
		}

		return "OK";

	} // del doIt


	private void raiseError(String string, String sql) throws Exception {
		
		// DB.rollback(false, get_TrxName());
		StringBuilder msg = new StringBuilder(string);
		ValueNamePair pp = CLogger.retrieveError();
		if (pp != null)
			msg = new StringBuilder(pp.getName()).append(" - ");
		msg.append(sql);
		throw new AdempiereUserError(msg.toString());
	}
	
	/**
	 * Returns a sql where string with the given category id and all of its subcategory ids.
	 * It is used as restriction in MQuery.
	 * @param productCategoryId
	 * @return
	 * @throws  
	 */
	private String getSubCategoryWhereClause(int productCategoryId) throws SQLException, AdempiereSystemError {
		//if a node with this id is found later in the search we have a loop in the tree
		int subTreeRootParentId = 0;
		StringBuilder retString = new StringBuilder();
		String sql = " SELECT M_Product_Category_ID, M_Product_Category_Parent_ID FROM M_Product_Category";
		final Vector<SimpleTreeNode> categories = new Vector<SimpleTreeNode>(100);
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = DB.createStatement();
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				if(rs.getInt(1)==productCategoryId) {
					subTreeRootParentId = rs.getInt(2);
				}
				categories.add(new SimpleTreeNode(rs.getInt(1), rs.getInt(2)));
			}
			retString.append(getSubCategoriesString(productCategoryId, categories, subTreeRootParentId));
		} catch (SQLException e) {
			throw e;
		} finally {
			DB.close(rs, stmt);
			rs = null; stmt = null;
		}
		return retString.toString();
	}

	/**
	 * Recursive search for subcategories with loop detection.
	 * @param productCategoryId
	 * @param categories
	 * @param loopIndicatorId
	 * @return comma separated list of category ids
	 * @throws AdempiereSystemError if a loop is detected
	 */
	private String getSubCategoriesString(int productCategoryId, Vector<SimpleTreeNode> categories, int loopIndicatorId) throws AdempiereSystemError {
		StringBuilder ret = new StringBuilder();
		final Iterator<SimpleTreeNode> iter = categories.iterator();
		while (iter.hasNext()) {
			SimpleTreeNode node = iter.next();
			if (node.getParentId() == productCategoryId) {
				if (node.getNodeId() == loopIndicatorId) {
					throw new AdempiereSystemError("The product category tree contains a loop on categoryId: " + loopIndicatorId);
				}
				ret.append(getSubCategoriesString(node.getNodeId(), categories, loopIndicatorId));
				ret.append(",");
			}
		}
		if (log.isLoggable(Level.FINE)) log.fine(ret.toString());
		return ret.toString() + productCategoryId;
	}

	/**
	 * Simple tree node class for product category tree search.
	 * @author Karsten Thiemann, kthiemann@adempiere.org
	 *
	 */
	private static class SimpleTreeNode {

		private int nodeId;

		private int parentId;

		public SimpleTreeNode(int nodeId, int parentId) {
			this.nodeId = nodeId;
			this.parentId = parentId;
		}

		public int getNodeId() {
			return nodeId;
		}

		public int getParentId() {
			return parentId;
		}
	}

 

} // M_PriceList_Create