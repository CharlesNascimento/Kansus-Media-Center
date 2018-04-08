package org.kansus.mediacenter.data;

import java.util.ArrayList;

import org.kansus.mediacenter.MyApplication;
import org.kansus.mediacenter.R;

public class RadioCategories {

	public static ArrayList<ListItem> mainCategories = new ArrayList<ListItem>();
	public static ArrayList<ListItem> musicCategories = new ArrayList<ListItem>();
	public static ArrayList<ListItem> sportsCategories = new ArrayList<ListItem>();

	static {
		// Main Categories
		String[] mainCategoriesNames = MyApplication.getAppContext().getResources().getStringArray(R.array.radio_stations_categories);
		mainCategories.add(new Category(mainCategoriesNames[0], R.drawable.ic_favorites));
		mainCategories.add(new Category(mainCategoriesNames[1], R.drawable.ic_recents));
		mainCategories.add(new Category(mainCategoriesNames[2], R.drawable.ic_my_radios));
		mainCategories.add(new Category(mainCategoriesNames[3], R.drawable.ic_local));
		mainCategories.add(new Category(mainCategoriesNames[4], R.drawable.ic_business));
		mainCategories.add(new Category(mainCategoriesNames[5], R.drawable.ic_comedy));
		mainCategories.add(new Category(mainCategoriesNames[6], R.drawable.ic_culture));
		mainCategories.add(new Category(mainCategoriesNames[7], R.drawable.ic_educational));
		mainCategories.add(new Category(mainCategoriesNames[8], R.drawable.ic_government));
		mainCategories.add(new Category(mainCategoriesNames[9], R.drawable.ic_music));
		mainCategories.add(new Category(mainCategoriesNames[10], R.drawable.ic_news));
		mainCategories.add(new Category(mainCategoriesNames[11], R.drawable.ic_sports));
		mainCategories.add(new Category(mainCategoriesNames[12], R.drawable.ic_technology));
		mainCategories.add(new Category(mainCategoriesNames[13], R.drawable.ic_varieties));
		mainCategories.add(new Category(mainCategoriesNames[14], R.drawable.ic_by_location));

		// Music Categories
		String[] musicCategoriesNames = MyApplication.getAppContext().getResources().getStringArray(R.array.radio_music_categories);
		for (int i = 0; i < musicCategoriesNames.length; i++) {
			Category item = new Category(musicCategoriesNames[i]);
			musicCategories.add(item);
		}

		// Sports Categories
		String[] sportCategoriesNames = MyApplication.getAppContext().getResources().getStringArray(R.array.radio_sports_categories);
		sportsCategories.add(new Category(sportCategoriesNames[0], R.drawable.ic_basketball));
		sportsCategories.add(new Category(sportCategoriesNames[1], R.drawable.ic_football));
		sportsCategories.add(new Category(sportCategoriesNames[2], R.drawable.ic_motoring));
		sportsCategories.add(new Category(sportCategoriesNames[3], R.drawable.ic_soccer));
	}
}
