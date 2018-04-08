package org.kansus.mediacenter.data;

public class Item extends ListItem {

	public String category;
	public String location;
	public String site;
	public String stream;
	public String thumbnail;

	public Item(String name, String category, String location, String site, String stream, String thumbnail) {
		this.name = name;
		this.category = category;
		this.location = location;
		this.site = site;
		this.stream = stream;
		this.thumbnail = thumbnail;
	}
}
