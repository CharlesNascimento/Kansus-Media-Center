package org.kansus.mediacenter.data;

import java.util.ArrayList;

import org.kansus.mediacenter.MyApplication;
import org.kansus.mediacenter.R;

public class TVCategories {

	public static ArrayList<ListItem> mainCategories = new ArrayList<ListItem>();

	static {
		// Main Categories
		String[] mainCategoriesNames = MyApplication.getAppContext().getResources().getStringArray(R.array.tv_stations_categories);
		mainCategories.add(new Category(mainCategoriesNames[0], R.drawable.ic_favorites));
		mainCategories.add(new Category(mainCategoriesNames[1], R.drawable.ic_recents));
		mainCategories.add(new Category(mainCategoriesNames[2], R.drawable.ic_my_radios));
		mainCategories.add(new Category(mainCategoriesNames[3], R.drawable.ic_local));
		mainCategories.add(new Category(mainCategoriesNames[4], R.drawable.ic_educational));
		mainCategories.add(new Category(mainCategoriesNames[5], R.drawable.ic_entertainment));
		mainCategories.add(new Category(mainCategoriesNames[6], R.drawable.ic_government));
		mainCategories.add(new Category(mainCategoriesNames[7], R.drawable.ic_kids));
		mainCategories.add(new Category(mainCategoriesNames[8], R.drawable.ic_lifestyle));
		mainCategories.add(new Category(mainCategoriesNames[9], R.drawable.ic_movies_series));
		mainCategories.add(new Category(mainCategoriesNames[10], R.drawable.ic_music));
		mainCategories.add(new Category(mainCategoriesNames[11], R.drawable.ic_news));
		mainCategories.add(new Category(mainCategoriesNames[12], R.drawable.ic_religious));
		mainCategories.add(new Category(mainCategoriesNames[13], R.drawable.ic_shopping));
		mainCategories.add(new Category(mainCategoriesNames[14], R.drawable.ic_sports));
		mainCategories.add(new Category(mainCategoriesNames[15], R.drawable.ic_varieties));
		mainCategories.add(new Category(mainCategoriesNames[16], R.drawable.ic_weather));
		mainCategories.add(new Category(mainCategoriesNames[17], R.drawable.ic_adult));
		mainCategories.add(new Category(mainCategoriesNames[18], R.drawable.ic_by_location));
	}
}
