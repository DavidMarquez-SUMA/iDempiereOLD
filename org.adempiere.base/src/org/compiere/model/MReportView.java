/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2012 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.compiere.model;
import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.util.CCache;
import org.compiere.util.CLogger;

public class MReportView extends X_AD_ReportView {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5674822764517548357L;
	
	/**	Static Logger					*/
	private static CLogger	s_log	= CLogger.getCLogger (MReportView.class);
	/**	Cache					*/
	static private CCache<Integer,MReportView> s_cache = new CCache<Integer,MReportView>(Table_Name, 30, 60);
	
	
	public MReportView(Properties ctx, int AD_ReportView_ID, String trxName) {
		super(ctx, AD_ReportView_ID, trxName);
	}
	
	public MReportView(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * 
	 * @param ctx
	 * @param AD_ReportView_ID
	 * @return
	 */
	public static MReportView get (Properties ctx, int AD_ReportView_ID) {
		if(AD_ReportView_ID==0) {
			return null;
		}
		
		Integer key = Integer.valueOf(AD_ReportView_ID);
		MReportView retValue = (MReportView)s_cache.get(key);
		if (retValue == null)
		{
			retValue = new MReportView (ctx, AD_ReportView_ID, null);
			s_cache.put(key, retValue);
		}
		return retValue;
	}	//	get
}
