package org.kansus.mediacenter.activity.music;

import android.app.ActivityGroup;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TabHost;

import org.kansus.mediacenter.R;

public class KansusMusic extends ActivityGroup {

	TabHost tabHost;
	//MediaStore mediaStore = new MediaStore();
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kansus_music);
        
        Resources res = getResources();
        tabHost = (TabHost)findViewById(R.id.tabhost);
        
        
        tabHost.setup(this.getLocalActivityManager());
        TabHost.TabSpec spec;
        Intent intent;
        
        Bundle bundle = new Bundle();
        
        
        //adc tab 1
        intent = new Intent().setClass(this, ArtistsActivity.class);
        spec = tabHost.newTabSpec("0").setIndicator("Artists", res.getDrawable(android.R.drawable.ic_menu_search)).setContent(intent);
        tabHost.addTab(spec);
        
        //adc tab 2
        intent = new Intent().setClass(this, AlbumsActivity.class);
        spec = tabHost.newTabSpec("0").setIndicator("Albums", res.getDrawable(android.R.drawable.ic_menu_search)).setContent(intent);
        tabHost.addTab(spec);
        
        //adc tab 3
        intent = new Intent().setClass(this, MusicActivity.class);
        intent.putExtras(bundle);
        spec = tabHost.newTabSpec("0").setIndicator("Songs", res.getDrawable(android.R.drawable.ic_menu_search)).setContent(intent);
        tabHost.addTab(spec);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // getMenuInflater().inflate(R.menu.activity_kansus_music, menu);
        return true;
    }
}
