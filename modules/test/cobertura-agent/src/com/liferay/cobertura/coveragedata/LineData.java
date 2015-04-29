/*
 * Cobertura - http://cobertura.sourceforge.net/
 *
 * Copyright (C) 2003 jcoverage ltd.
 * Copyright (C) 2005 Mark Doliner
 * Copyright (C) 2005 Mark Sinke
 * Copyright (C) 2006 Jiri Mares
 *
 * Cobertura is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * Cobertura is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cobertura; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA
 */

package com.liferay.cobertura.coveragedata;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 * This class implements HasBeenInstrumented so that when cobertura
 * instruments itself, it will omit this class.  It does this to
 * avoid an infinite recursion problem because instrumented classes
 * make use of this class.
 * </p>
 */
public class LineData
		implements Comparable, CoverageData, Serializable
{
	private static final long serialVersionUID = 4;

	private transient Lock lock;

	private long hits;
	private final ConcurrentMap<Integer, JumpData> _jumpDatas =
		new ConcurrentHashMap<>();
	private final ConcurrentMap<Integer, SwitchData> _switchDatas =
		new ConcurrentHashMap<>();
	private final int lineNumber;

	public LineData(int lineNumber)
	{
		this.hits = 0;
		this.lineNumber = lineNumber;
		initLock();
	}

	private void initLock()
	{
		 lock = new ReentrantLock();
	}

	/**
	 * This is required because we implement Comparable.
	 */
	public int compareTo(Object o)
	{
		if (!o.getClass().equals(LineData.class))
			return Integer.MAX_VALUE;
		return this.lineNumber - ((LineData)o).lineNumber;
	}

	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if ((obj == null) || !(obj.getClass().equals(this.getClass())))
			return false;

		LineData lineData = (LineData)obj;
		getBothLocks(lineData);
		try
		{
			return (this.hits == lineData.hits)
					&& ((this._jumpDatas == lineData._jumpDatas) || (this._jumpDatas.equals(lineData._jumpDatas)))
					&& ((this._switchDatas == lineData._switchDatas) || ((this._switchDatas != null) && (this._switchDatas.equals(lineData._switchDatas))))
					&& (this.lineNumber == lineData.lineNumber);
		}
		finally
		{
			lock.unlock();
			lineData.lock.unlock();
		}
	}

	public double getBranchCoverageRate()
	{
		if (getNumberOfValidBranches() == 0)
			return 1d;
		lock.lock();
		try
		{
			return ((double) getNumberOfCoveredBranches()) / getNumberOfValidBranches();
		}
		finally
		{
			lock.unlock();
		}
	}

	public boolean isCovered()
	{
		lock.lock();
		try
		{
			return (hits > 0) && ((getNumberOfValidBranches() == 0) || ((1.0 - getBranchCoverageRate()) < 0.0001));
		}
		finally
		{
			lock.unlock();
		}
	}

	public double getLineCoverageRate()
	{
		return (hits > 0) ? 1 : 0;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	/**
	 * @see net.sourceforge.cobertura.coveragedata.CoverageData#getNumberOfCoveredBranches()
	 */
	/*public int getNumberOfCoveredBranches()
	{
		if (this.branches == null)
			return 0;
		int covered = 0;
		for (Iterator i = this.branches.iterator(); i.hasNext(); covered += ((BranchData) i.next()).getNumberOfCoveredBranches());
		return covered;
	}*/

	public int getNumberOfCoveredLines()
	{
		return (hits > 0) ? 1 : 0;
	}

	public int getNumberOfValidBranches()
	{
		int ret = 0;
		lock.lock();
		try
		{
			for (JumpData jumpData : _jumpDatas.values()) {
				ret += jumpData.getNumberOfValidBranches();
			}

			for (SwitchData switchData : _switchDatas.values()) {
				ret += switchData.getNumberOfValidBranches();
			}

			return ret;
		}
		finally
		{
			lock.unlock();
		}
	}

	public int getNumberOfCoveredBranches()
	{
		int ret = 0;
		lock.lock();
		try
		{
			for (JumpData jumpData : _jumpDatas.values()) {
				ret += jumpData.getNumberOfCoveredBranches();
			}

			for (SwitchData switchData : _switchDatas.values()) {
				ret += switchData.getNumberOfCoveredBranches();
			}

			return ret;
		}
		finally
		{
			lock.unlock();
		}
	}

	public int getNumberOfValidLines()
	{
		return 1;
	}

	public int hashCode()
	{
		return this.lineNumber;
	}

	public void merge(CoverageData coverageData)
	{
		LineData lineData = (LineData)coverageData;
		getBothLocks(lineData);
		try
		{
			this.hits += lineData.hits;

			ConcurrentMap<Integer, JumpData> otherJumpDatas =
				lineData._jumpDatas;

			for (JumpData jumpData : otherJumpDatas.values()) {
				JumpData previousJumpData = _jumpDatas.putIfAbsent(
					jumpData.getConditionNumber(), jumpData);

				if (previousJumpData != null) {
					previousJumpData.merge(jumpData);
				}
			}

			ConcurrentMap<Integer, SwitchData> otherSwitchDatas =
				lineData._switchDatas;

			for (SwitchData switchData : otherSwitchDatas.values()) {
				SwitchData previousSwitchData = _switchDatas.putIfAbsent(
					switchData.getSwitchNumber(), switchData);

				if (previousSwitchData != null) {
					previousSwitchData.merge(switchData);
				}
			}
		}
		finally
		{
			lock.unlock();
			lineData.lock.unlock();
		}
	}

	public JumpData addJump(JumpData jumpData) {
		JumpData previousJumpData = _jumpDatas.putIfAbsent(
			jumpData.getConditionNumber(), jumpData);

		if (previousJumpData != null) {
			return previousJumpData;
		}

		return jumpData;
	}

	public SwitchData addSwitch(SwitchData switchData) {
		SwitchData previousSwitchData = _switchDatas.putIfAbsent(
			switchData.getSwitchNumber(), switchData);

		if (previousSwitchData != null) {
			return previousSwitchData;
		}

		return switchData;
	}

	void touch(int new_hits)
	{
		lock.lock();
		try
		{
			this.hits+=new_hits;
		}
		finally
		{
			lock.unlock();
		}
	}

	public void touchJump(
		String className, int jumpNumber, boolean branch,int hits) {

		JumpData jumpData = _jumpDatas.get(jumpNumber);

		if (jumpData == null) {
			throw new IllegalStateException(
				"No instrument data for class " + className + " line " +
					lineNumber + " jump " + jumpNumber);
		}

		jumpData.touchBranch(branch, hits);
	}

	public void touchSwitch(
		String className, int switchNumber, int branch,int hits) {

		SwitchData switchData = _switchDatas.get(switchNumber);

		if (switchData == null) {
			throw new IllegalStateException(
				"No instrument data for class " + className + " line " +
					lineNumber + " switch " + switchNumber);
		}

		switchData.touchBranch(className, lineNumber, branch, hits);
	}

	private void getBothLocks(LineData other) {
		/*
		 * To prevent deadlock, we need to get both locks or none at all.
		 *
		 * When this method returns, the thread will have both locks.
		 * Make sure you unlock them!
		 */
		boolean myLock = false;
		boolean otherLock = false;
		while ((!myLock) || (!otherLock))
		{
			try
			{
				myLock = lock.tryLock();
				otherLock = other.lock.tryLock();
			}
			finally
			{
				if ((!myLock) || (!otherLock))
				{
					//could not obtain both locks - so unlock the one we got.
					if (myLock)
					{
						lock.unlock();
					}
					if (otherLock)
					{
						other.lock.unlock();
					}
					//do a yield so the other threads will get to work.
					Thread.yield();
				}
			}
		}
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		in.defaultReadObject();
		initLock();
	}
}
