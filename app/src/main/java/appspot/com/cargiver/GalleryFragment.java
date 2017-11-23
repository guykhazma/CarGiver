package appspot.com.cargiver;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import appspot.com.cargiver.R;
/**
 * Created by Guy on 11/22/2017.
 */

public class GalleryFragment extends Fragment {
    // Inflate the layout for this fragment
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.gallery_fragment, container, false);
    }
}
