package com.rockit.ip.healthmon;

import java.util.Date;

import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbXMLNSC;

public class JksCheck {

	public static void getJksInfo(MbElement[] resultTree, MbElement[] resultErrorTree, String trustStoreFileName, String trustStorePwd) {
		MbElement root = resultTree[0];
		MbElement rootError = resultErrorTree[0];
		String certificateStatus = null;
		try {
			JksUtils utils = new JksUtils(trustStoreFileName,
					trustStorePwd);

			for (String alias : utils.getCerts().keySet()) {
				MbElement item = root.createElementAsLastChild(MbXMLNSC.FOLDER,
						"Certificate", null);
				item.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,
						"SubjectDN", utils.getCerts().get(alias)
						.getSubjectDN().toString());
				item.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,
						"Alias", alias);
				item.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,
						"valid_from", utils.getCerts().get(alias).getNotBefore().toString());
				item.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,
						"valid_to", utils.getCerts().get(alias).getNotAfter().toString());
				if(utils.getCerts().get(alias).getNotAfter().after(new Date())){
					certificateStatus = "OK";
				}else{
					certificateStatus = "NOK";
				}
				item.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,
						"CertificateStatus", certificateStatus);
			}
		} catch (Exception ex) {
			try {
				MbElement itemError = rootError.createElementAsLastChild(MbXMLNSC.FOLDER,
						"error", null);
				itemError.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,
						"errorMessage", ex.getMessage());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			throw new RuntimeException(ex);
		}
	}


}
