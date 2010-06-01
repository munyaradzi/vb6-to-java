/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package buggymastercode;

import java.util.List;

import org.apache.commons.beanutils.DynaBean;


/**
 *
 * @author jalvarez
 */
public class DBRecordSet {

	private List<DynaBean> m_rows;

	public void setRows(List<DynaBean> rows) { m_rows = rows;}
	public List<DynaBean> getRows() { return m_rows;}
}
