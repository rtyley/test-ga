package com.github.mobile.gauges.core;

import java.util.Date;

/**
 * Dated view summary
 */
public class DatedViewSummary extends ViewSummary {

	private Date date;

	/**
	 * @return date
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * @param date
	 * @return this summary
	 */
	public DatedViewSummary setDate(Date date) {
		this.date = date;
		return this;
	}
}
