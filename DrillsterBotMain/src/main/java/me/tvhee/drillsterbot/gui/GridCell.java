package me.tvhee.drillsterbot.gui;

import java.awt.*;

public final class GridCell
{
	private final int x;
	private final int y;
	private int insets;
	private double weightX = -1;
	private double weightY = -1;
	private int sizeX = 1;
	private int sizeY = 1;
	private Fill fill;

	public GridCell(int x, int y)
	{
		this.x = x;
		this.y = y;
	}

	public GridCell setSize(int width, int height)
	{
		this.sizeX = width;
		this.sizeY = height;
		return this;
	}

	public GridCell setWeight(double x, double y)
	{
		this.weightX = x;
		this.weightY = y;
		return this;
	}
	
	public GridCell setInsets(int width)
	{
		this.insets = width;
		return this;
	}

	public GridCell setFill(Fill fill)
	{
		this.fill = fill;
		return this;
	}

	public GridBagConstraints toConstraints()
	{
		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = x;
		gridBagConstraints.gridy = y;

		if(sizeX != -1 && sizeY != -1)
		{
			gridBagConstraints.gridwidth = sizeX;
			gridBagConstraints.gridheight = sizeY;
		}

		if(weightX != -1 && weightY != -1)
		{
			gridBagConstraints.weightx = this.weightX;
			gridBagConstraints.weighty = this.weightY;
		}
		
		if(insets != 0)
			gridBagConstraints.insets = new Insets(insets, insets, insets, insets);

		if(fill != null)
			gridBagConstraints.fill = fill.getCode();

		return gridBagConstraints;
	}

	public enum Fill
	{
		NONE(0), HORIZONTAL(2), VERTICAL(3), BOTH(1);

		private final int code;

		Fill(int code)
		{
			this.code = code;
		}

		public int getCode()
		{
			return code;
		}
	}
}
