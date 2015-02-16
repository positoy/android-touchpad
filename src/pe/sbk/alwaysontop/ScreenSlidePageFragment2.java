/*
 * Copyright 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pe.sbk.alwaysontop;

import pe.sbk.alwaysontop.AlwaysOnTopService;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
/**
 * A fragment representing a single step in a wizard. The fragment shows a dummy title indicating
 * the page number, along with some dummy text.
 *
 * <p>This class is used by the {@link CardFlipActivity} and {@link
 * ScreenSlideActivity} samples.</p>
 */
public class ScreenSlidePageFragment2 extends Fragment{
    /**
     * The argument key for the page number this fragment represents.
     */
	
    public static SharedPreferences settings;
	public static SharedPreferences.Editor settingsEdit;
    public static final String ARG_PAGE = "page";
    public ImageView introImage;
    public Button close;
    /**
     * The fragment's page number, which is set to the argument value for {@link #ARG_PAGE}.
     */
    private int mPageNumber;

    /**
     * Factory method for this fragment class. Constructs a new fragment for the given page number.
     */
    public static ScreenSlidePageFragment2 create(int pageNumber) {
    	
        ScreenSlidePageFragment2 fragment = new ScreenSlidePageFragment2();
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, pageNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public ScreenSlidePageFragment2() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageNumber = getArguments().getInt(ARG_PAGE);
        settings = getActivity().getSharedPreferences("first", 0);
		settingsEdit = settings.edit();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout containing a title and body text.
        ViewGroup rootView = (ViewGroup) inflater
                .inflate(R.layout.fragment_screen_slide_page2, container, false);
        		
        introImage = (ImageView) rootView.findViewById(R.id.intro);
        close = (Button) rootView.findViewById(R.id.intro_close);
        //close.setOnClickListener((OnClickListener) getActivity());

        switch(mPageNumber) {
        case 0:
        	introImage.setImageResource(R.drawable.tutorial1);
        	break;
        case 1:
        	introImage.setImageResource(R.drawable.tutorial2);
        	break;
        case 2:
        	introImage.setImageResource(R.drawable.tutorial3);        
        	close.setVisibility(rootView.VISIBLE);
        	startService();
        	break;       
        }

        return rootView;
    }

    /**
     * Returns the page number represented by this fragment object.
     */
    public int getPageNumber() {
        return mPageNumber;
    }
    
    public void startService(){
    	close.setOnClickListener(new OnClickListener() {
    		@Override
    		public void onClick(View v) {
    			if(v.getId() == R.id.intro_close) {
    		    	getActivity().startService(new Intent(getActivity(), AlwaysOnTopService.class));
    		    	settingsEdit.putBoolean("first",true);
    	        	settingsEdit.commit();
    		    	getActivity().finish();
    				onDetach();
    			}
    		}
    	}); 
    	
    }
	
}
