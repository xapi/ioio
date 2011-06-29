package ioio.manager;

import ioio.manager.FirmwareManager.ImageFile;

import java.io.File;
import java.io.IOException;

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class FirmwareActivity extends ListActivity {
	private static final String TAG = "FirmwareActivity";
	private static final int ADD_FROM_FILE = 0;
	private static final int ADD_FROM_QR = 1;
	FirmwareManager firmwareManager_;
	private ListAdapter listAdapter_;
	private ioio.manager.FirmwareManager.Bundle[] bundles_;

	private class ListAdapter extends BaseAdapter {

		@Override
		public void notifyDataSetChanged() {
			bundles_ = firmwareManager_.getAppBundles();
			super.notifyDataSetChanged();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final int pos = position;
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(R.layout.list_item,
						parent, false);
			}
			convertView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					FirmwareActivity.this.onClick(v, pos);
				}

			});
			ioio.manager.FirmwareManager.Bundle bundle = bundles_[position];
			((RadioButton) convertView.findViewById(R.id.checked))
					.setChecked(bundle.isActive());
			((TextView) convertView.findViewById(R.id.title)).setText(bundle
					.getName());
			ImageFile[] images = bundle.getImages();
			StringBuffer strbuf = new StringBuffer();
			for (int i = 0; i < images.length; ++i) {
				if (i != 0) {
					strbuf.append(' ');
				}
				strbuf.append(images[i].getName());
			}
			((TextView) convertView.findViewById(R.id.content)).setText(strbuf
					.toString());
			return convertView;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public Object getItem(int position) {
			return bundles_[position];
		}

		@Override
		public int getCount() {
			return bundles_.length;
		}

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		try {
			firmwareManager_ = new FirmwareManager(this);
			bundles_ = firmwareManager_.getAppBundles();
			listAdapter_ = new ListAdapter();
			setListAdapter(listAdapter_);
			registerForContextMenu(getListView());
		} catch (IOException e) {
			Log.w(TAG, e);
		}
		Intent intent = getIntent();
		if (intent.getAction().equals(Intent.ACTION_VIEW)
				&& intent.getScheme().equals("file")) {
			addBundleFromFile(new File(intent.getData().getPath()));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.firmware_options, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.addByScanMenuItem:
			addByScan();
			return true;
		case R.id.addFromStorageMenuItem:
			addFromExternalStorage();
			return true;
		case R.id.clearActiveBundle:
			clearActiveBundle();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void clearActiveBundle() {
		try {
			firmwareManager_.clearActiveBundle();
			listAdapter_.notifyDataSetChanged();
			Toast.makeText(this, "Active bundle cleared", Toast.LENGTH_SHORT)
					.show();
		} catch (IOException e) {
			Log.w(TAG, e);
		}
	}

	private void addByScan() {
		Intent intent = new Intent("com.google.zxing.client.android.SCAN");
		intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
		try {
			startActivityForResult(intent, ADD_FROM_QR);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this, "Please install the ZXing barcode scanner",
					Toast.LENGTH_SHORT).show();
		}
	}

	private void addFromExternalStorage() {
		startActivityForResult(new Intent(this, SelectFileActivity.class),
				ADD_FROM_FILE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case ADD_FROM_FILE:
			if (resultCode == RESULT_OK) {
				File file = (File) data
				.getSerializableExtra(FileReturner.SELECTED_FILE_EXTRA);
				addBundleFromFile(file);
			} else if (resultCode == FileReturner.RESULT_ERROR) {
				Toast.makeText(
						this,
						"Error: "
								+ data.getStringExtra(FileReturner.ERROR_MESSAGE_EXTRA),
						Toast.LENGTH_LONG).show();
			}
			break;

		case ADD_FROM_QR:
			if (resultCode == RESULT_OK) {
				try {
					String contents = data.getStringExtra("SCAN_RESULT");
					String format = data.getStringExtra("SCAN_RESULT_FORMAT");
					if (format.equals("QR_CODE")) {
						Intent intent = new Intent(this,
								DownloadUrlActivity.class);
						intent.putExtra(DownloadUrlActivity.URL_EXTRA, contents);
						startActivityForResult(intent, ADD_FROM_FILE);

					} else {
						Toast.makeText(this,
								"Invalid barcode - expecting a URI",
								Toast.LENGTH_LONG).show();
					}
					// File file = (File) data
					// .getSerializableExtra(SelectFileActivity.SELECTED_FILE_EXTRA);
					// firmwareManager_.addAppBundle(file.getAbsolutePath());
					// listAdapter_.notifyDataSetChanged();
					// Toast.makeText(this,
					// "Format: " + format + "\nContents: " + contents,
					// Toast.LENGTH_LONG).show();
				} catch (Exception e) {
					Log.w(TAG, e);
					Toast.makeText(this,
							"Failed to add bundle: " + e.getMessage(),
							Toast.LENGTH_LONG).show();
				}
			}
			break;
		}
	}

	private void addBundleFromFile(File file) {
		try {
			ioio.manager.FirmwareManager.Bundle bundle = firmwareManager_
					.addAppBundle(file.getAbsolutePath());
			listAdapter_.notifyDataSetChanged();
			Toast.makeText(this, "Bundle added: " + bundle.getName(),
					Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			Log.w(TAG, e);
			Toast.makeText(this,
					"Failed to add bundle: " + e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.list_item_context_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.remove_item:
			try {
				String name = bundles_[(int) info.id].getName();
				firmwareManager_.removeAppBundle(name);
				listAdapter_.notifyDataSetChanged();
				Toast.makeText(this, "Bundle removed: " + name,
						Toast.LENGTH_SHORT).show();
				return true;
			} catch (IOException e) {
				Log.w(TAG, e);
				Toast.makeText(this,
						"Failed to remove bundle: " + e.getMessage(),
						Toast.LENGTH_SHORT).show();
			}
		default:
			return super.onContextItemSelected(item);
		}
	}

	private void onClick(View v, int pos) {
		ioio.manager.FirmwareManager.Bundle bundle = (ioio.manager.FirmwareManager.Bundle) getListView()
				.getItemAtPosition(pos);
		try {
			firmwareManager_.clearActiveBundle();
			firmwareManager_.setActiveAppBundle(bundle.getName());
			listAdapter_.notifyDataSetChanged();
			Toast.makeText(this, "Bundle set as active: " + bundle.getName(),
					Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
			Log.w(TAG, e);
		}
	}
}