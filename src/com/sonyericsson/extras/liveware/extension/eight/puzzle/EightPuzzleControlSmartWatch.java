/*
Copyright (c) 2012 Sony Ericsson Mobile Communications AB
Copyright (c) 2012 Sony Mobile Communications AB.

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

 * Neither the name of the Sony Mobile Communications AB nor the names
  of its contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.sonyericsson.extras.liveware.extension.eight.puzzle;

import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.extension.util.Dbg;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.control.ControlTouchEvent;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;


import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.support.v4.app.NavUtils;

/**
 * Control extension for 8 Game for SmartWatch
 */
class EightPuzzleControlSmartWatch extends ControlExtension {

	private static final int SHOW_SOLVED_IMAGE_TIME = 1500;

	private static final Bitmap.Config BITMAP_CONFIG = Bitmap.Config.RGB_565;

	private static final int NUMBER_TILE_TEXT_SIZE = 24;

	private static final int RANDOM_START_MOVES = 100;

	private ArrayList<GameTile> mGameTiles;

	private int mNumberOfMoves;

	// tile positions (row major)
	private ArrayList<TilePosition> mTilePositions;

	private int mEmptyTileIndex;

	private TextPaint mNumberTextPaint;

	private Bitmap mCurrentImage = null;

	/** Bitmap used when showing finished image game */
	private Bitmap mFullImage = null;

	private int mTilePressIndex = -1;

	private boolean mLongPressed = false;

	private GameType mGameType = GameType.NUMBERS;

	private Handler mHandler = null;

	private GameState mGameState = GameState.PLAYING;

	/** Lower left button rectangle */
	private static final Rect sActionButton1Rect = new Rect(0, 88, 40, 128);

	/** Lower middle button rectangle */
	private static final Rect sActionButton2Rect = new Rect(44, 88, 84, 128);

	/** Lower right button rectangle */
	private static final Rect sActionButton3Rect = new Rect(88, 88, 128, 128);

	private Rect mPressedButtonRect = null;

	private int mPressedActionImageId;

	private int mPressedActionDrawableId;

	private int mWidth;

	private int mHeight;

	//int m_nScreenW = 0, m_nScreenH = 0;

	int pressedX = -1, pressedY = -1;
	
	long pressedTime=-1;
	
	boolean cursorOn = false;

	boolean longpress = false;
	
	long longpressStart = -1;
	
	boolean dragMode = false;
	
	private boolean m_bRunning = false;
	
	

	private enum GameType {
		IMAGE, NUMBERS
	}

	private enum GameState {
		PLAYING, FINISHED_SHOW_IMAGE, FINISHED_SHOW_MENU, ACTION_MENU,
	}

	/**
	 * Create eight puzzle control.
	 *
	 * @param hostAppPackageName Package name of host application.
	 * @param context The context.
	 * @param handler The handler to use
	 */
	EightPuzzleControlSmartWatch(final String hostAppPackageName, final Context context,
			Handler handler) {
		super(context, hostAppPackageName);
		if (handler == null) {
			throw new IllegalArgumentException("handler == null");
		}
		mHandler = handler;
		mNumberTextPaint = new TextPaint();
		mNumberTextPaint.setTextSize(NUMBER_TILE_TEXT_SIZE);
		mNumberTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
		mNumberTextPaint.setColor(Color.WHITE);
		mNumberTextPaint.setAntiAlias(true);

		mWidth = getSupportedControlWidth(context);
		mHeight = getSupportedControlHeight(context);
		cmdTurnCursorServiceOn();	
		Log.d("Eight", "XYZZY: Con");
	}

	/**
	 * Get supported control width.
	 *
	 * @param context The context.
	 * @return the width.
	 */
	public static int getSupportedControlWidth(Context context) {
		return context.getResources().getDimensionPixelSize(R.dimen.smart_watch_control_width);
	}

	/**
	 * Get supported control height.
	 *
	 * @param context The context.
	 * @return the height.
	 */
	public static int getSupportedControlHeight(Context context) {
		return context.getResources().getDimensionPixelSize(R.dimen.smart_watch_control_height);
	}

	@Override
	public void onDestroy() {
		Dbg.d("NavigationControlLiveViewTouch onDestroy");

		if (mContext != null) {
			//mHandler.removeCallbacks(mDrawResult);
			//mHandler.removeCallbacks(mDrawActionScreen);
		}
		cmdTurnCursorServiceOff();
		cursorOn = false;
		Log.d("Eight", "XYZZY: Des");
	};

	@Override
	public void onStart() {
		Dbg.d("onStart");
		mGameState = GameState.PLAYING;
		Log.d("Eight", "XYZZY: Sta");
	}

	/**
	 * Start new game.
	 */
	private void startNewGame() {
		drawLoadingScreen();

		// Create game positions
		initTilePositions(new TilePosition(1, new Rect(2, 2, 42, 42)), new TilePosition(2,
				new Rect(44, 2, 84, 42)), new TilePosition(3, new Rect(86, 2, 126, 42)),
				new TilePosition(4, new Rect(2, 44, 42, 84)), new TilePosition(5, new Rect(44, 44,
						84, 84)), new TilePosition(6, new Rect(86, 44, 126, 84)), new TilePosition(
								7, new Rect(2, 86, 42, 126)),
								new TilePosition(8, new Rect(44, 86, 84, 126)), new TilePosition(9, new Rect(86,
										86, 126, 126)));

		// Create game image
		if (mGameType == GameType.IMAGE) {
			mCurrentImage = getRandomImage();
			mFullImage = mCurrentImage;
			if (mCurrentImage == null) {
				Dbg.w("initTiles: Failed to get image, change to number game");
				mGameType = GameType.NUMBERS;
				mCurrentImage = getNumberImage();
			}
		} else {
			mCurrentImage = getNumberImage();
		}

		// Create game tiles
		initTiles();

		// Do RANDOM_START_MOVES random moves
		Random random = new Random();
		for (int i = 0; i < RANDOM_START_MOVES; i++) {
			// Get random movable tile index
			ArrayList<Integer> indices = getMovableTileIndices(mEmptyTileIndex);
			Dbg.d("Movable indices: " + indices.toString());
			if (indices.size() == 0) {
				Dbg.e("Invalid empty tile index!");
				break;
			}
			int movingIndex = indices.get(random.nextInt(indices.size()));
			Dbg.d("Moving " + movingIndex);

			// Swap frames in mGameTiles
			GameTile tile1 = mGameTiles.get(movingIndex - 1);
			GameTile tile2 = mGameTiles.get(mEmptyTileIndex - 1);

			Rect tmpFrame = tile1.tilePosition.frame;
			tile1.tilePosition.frame = tile2.tilePosition.frame;
			tile2.tilePosition.frame = tmpFrame;

			mGameTiles.set(mEmptyTileIndex - 1, tile1);
			mGameTiles.set(movingIndex - 1, tile2);

			// Set mEmptyTileIndex
			Dbg.d("Changing empty index from " + mEmptyTileIndex + " to " + movingIndex);
			mEmptyTileIndex = movingIndex;
		}

		// Draw initial game Bitmap
		getCurrentImage(true);

		// Init game state
		mNumberOfMoves = 0;
		mGameState = GameState.PLAYING;
		Dbg.d("game started with empty tile index " + mEmptyTileIndex);
	}

	/**
	 * Draw all tiles into bitmap and show it.
	 *
	 * @param show True if bitmap shown be shown, false otherwise
	 * @return The complete bitmap of the current game
	 */
	private Bitmap getCurrentImage(boolean show) {
		Bitmap bitmap = Bitmap.createBitmap(mWidth, mHeight, BITMAP_CONFIG);
		bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
		Canvas canvas = new Canvas(bitmap);
		// Set background
		canvas.drawColor(Color.WHITE);
		// Draw tiles
		for (GameTile gt : mGameTiles) {
			canvas.drawBitmap(gt.bitmap, gt.tilePosition.frame.left, gt.tilePosition.frame.top,
					null);
		}
		if (show) {
			showBitmap(bitmap);
		}

		return bitmap;
	}

	/**
	 * Set bitmap within tile object
	 *
	 * @param source The bitmap
	 * @param gt The tile
	 */
	private void setTileBitmap(Bitmap source, GameTile gt) {
		Rect frame = gt.tilePosition.frame;
		for (TilePosition tilePosition : mTilePositions) {
			if (tilePosition.position == gt.correctPosition) {
				frame = tilePosition.frame;
				break;
			}
		}

		gt.bitmap = Bitmap.createBitmap(source, frame.left, frame.top, frame.width(),
				frame.height());
		// Set the density to default to avoid scaling.
		gt.bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
	}

	/**
	 * Get bitmap with number tiles drawn.
	 *
	 * @return The bitmap
	 */
	private Bitmap getNumberImage() {
		Bitmap bitmap = Bitmap.createBitmap(mWidth, mHeight, BITMAP_CONFIG);
		// Set the density to default to avoid scaling.
		bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);

		Canvas canvas = new Canvas(bitmap);
		canvas.drawColor(Color.WHITE);

		Paint tilePaint = new Paint();
		tilePaint.setColor(mContext.getResources().getColor(R.color.color_number_tile_background));
		for (TilePosition tilePosition : mTilePositions) {
			if (tilePosition.position != 9) {
				canvas.drawRect(tilePosition.frame, tilePaint);
				canvas.drawText(Integer.toString(tilePosition.position),
						tilePosition.frame.left + 13, tilePosition.frame.top + 29, mNumberTextPaint);
			}
		}

		return bitmap;
	}

	/**
	 * Get bitmap with image tiles drawn.
	 *
	 * @return The bitmap
	 */
	private Bitmap getRandomImage() {
		Dbg.d("getRandomImage");
		Bitmap bitmap = null;

		Cursor cursor = null;
		try {
			cursor = mContext.getContentResolver().query(
					MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
					new String[] {
							Images.Media._ID, Images.Media.DATA,
							MediaStore.Images.Media.ORIENTATION
					}, null, null, null);

			if (cursor != null && cursor.getCount() > 0) {
				// Move to random index in cursor
				int cursorPosition = new Random(System.currentTimeMillis()).nextInt(cursor
						.getCount());
				Dbg.d("cursorIndex " + cursorPosition + ", cursor.getCount(): " + cursor.getCount());
				cursor.moveToPosition(cursorPosition);

				int dataIndex = cursor.getColumnIndex(Images.Media.DATA);
				String data = cursor.getString(dataIndex);

				int imageIdIndex = cursor.getColumnIndex(Images.Media._ID);
				long imageId = cursor.getLong(imageIdIndex);

				int orientationIndex = cursor
						.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION);
				int orientation = cursor.getInt(orientationIndex);

				Dbg.d("data: " + data + ", orientation: " + orientation + ", imageId: " + imageId);

				bitmap = getScaledBitmap(data, mWidth, mHeight);
				if (bitmap == null) {
					Dbg.e("getRandomImage failed: bitmap == null");
					return null;
				}
				Dbg.d("width: " + bitmap.getWidth() + ", height: " + bitmap.getHeight());

				bitmap = scaleAndRotate(bitmap, mWidth, mHeight, orientation);
				bitmap = squareAndCenter(bitmap);
				Dbg.d("Scaled width: " + bitmap.getWidth() + ", height: " + bitmap.getHeight());

			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return bitmap;
	}

	/**
	 * Decode image into bitmap that fits screen.
	 *
	 * @param imagePath The image path
	 * @param orientation The orientation
	 * @param displayWidth The display width
	 * @param displayHeight The display height
	 * @return the scaled and rotated bitmap
	 */
	private static Bitmap getScaledBitmap(String imagePath, int displayWidth, int displayHeight) {
		// Decode image size to see how small we can make the image.
		// Ref:
		// http://stackoverflow.com/questions/477572/android-strange-out-of-memory-issue
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(imagePath, options);

		// Scale image to fill screen
		int scale;
		int imageWidth = options.outWidth;
		int imageHeight = options.outHeight;
		if (imageWidth > imageHeight) {
			// landscape, crop width
			scale = imageHeight / displayHeight;
		} else {
			// portrait, crop height
			scale = imageWidth / displayWidth;
		}

		// Find nearest smaller scale that is a power of 2
		// Formula: scale = 2^floor(log2(scale))
		Dbg.d("scale before " + scale);
		scale = (int)Math.pow(2, Math.floor(Math.log(scale) / Math.log(2)));
		Dbg.d("scale after " + scale);

		// Decode with inSampleSize
		BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
		decodeOptions.inSampleSize = scale;
		decodeOptions.inPreferredConfig = BITMAP_CONFIG;
		Bitmap bitmap = BitmapFactory.decodeFile(imagePath, decodeOptions);
		return bitmap;
	}

	/**
	 * Scale and rotate bitmap using the input size and rotation.
	 *
	 * @param bitmapOrg The bitmap to scale and rotate
	 * @param newWidth The desired new width
	 * @param newHeight The desired new height
	 * @param rotateDegrees The number of degrees to rotate
	 * @return The new bitmap
	 */
	private static Bitmap scaleAndRotate(Bitmap bitmapOrg, int newWidth, int newHeight,
			int rotateDegrees) {
		int width = bitmapOrg.getWidth();
		int height = bitmapOrg.getHeight();

		// Calculate the scale - in this case = 0.4f
		float scaleWidth = ((float)newWidth) / width;
		float scaleHeight = ((float)newHeight) / height;

		// Create matrix for the manipulation
		Matrix matrix = new Matrix();
		// Resize the bit map
		matrix.postScale(scaleWidth, scaleHeight);
		// Rotate the Bitmap
		matrix.postRotate(rotateDegrees);

		// Recreate the new Bitmap
		Bitmap scaled = Bitmap.createBitmap(bitmapOrg, 0, 0, width, height, matrix, true);
		// Set the density to default to avoid scaling.
		scaled.setDensity(DisplayMetrics.DENSITY_DEFAULT);
		if (scaled != bitmapOrg) {
			// createBitmap may return the same bitmap. Only recycle old if
			// new image is returned.
			bitmapOrg.recycle();
		}
		return scaled;
	}

	/**
	 * Crop image to a square. Keep smallest value of width, height
	 *
	 * @param bitmap The bitmap
	 */
	private static Bitmap squareAndCenter(Bitmap bitmap) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		int x = 0;
		int y = 0;

		if (width == height) {
			return bitmap;
		}

		if (width > height) {
			x = (width - height) / 2;
			width = height;
		} else if (height > width) {
			y = (height - width) / 2;
			height = width;
		}

		// Crop image
		Bitmap scaled = Bitmap.createBitmap(bitmap, x, y, width, height);
		// Set the density to default to avoid scaling.
		scaled.setDensity(DisplayMetrics.DENSITY_DEFAULT);
		if (scaled != bitmap) {
			// createBitmap may return the same bitmap. Only recycle old if
			// new image is returned.
			bitmap.recycle();
		}

		return scaled;
	}

	@Override
	public void onStop() {
		Dbg.d("onStop");
		Log.d("Eight", "XYZZY: Sto");
	}

	/*
	 * 	HELPER CURSOR FUNCTIONS
	 */
	private void cmdTurnCursorServiceOn() {	
		Intent i = new Intent();
		i.setAction("com.sonyericsson.extras.liveware.extension.eight.puzzle.CursorService");
		mContext.startService(i);
		//startService(i);
	}

	private void cmdTurnCursorServiceOff() {
		Intent i = new Intent();
		i.setAction("com.sonyericsson.extras.liveware.extension.eight.puzzle.CursorService");
		mContext.stopService(i);
	}

	private void cmdShowCursor() {
		if (Singleton.getInstance().m_CurService != null)
			Singleton.getInstance().m_CurService.ShowCursor(true);
	}

	private void cmdHideCursor() {
		if (Singleton.getInstance().m_CurService != null)
			Singleton.getInstance().m_CurService.ShowCursor(false);
	}


	private void cursorOn() {
		/*if (m_bRunning) return;
		Log.d("GUI", "Start clicked!");
		// show cursor*/
		cmdShowCursor();
		Singleton.getInstance().m_CurService.Update(1, 1, true);
		// start a thread to move cursor arround
		/*m_bRunning = true;

		Thread t = new Thread() {
			int last_x = 0, last_y = 0;
			public void run() 
		        {		
					while (m_bRunning) {
						Random rnd = new Random();
						int new_x = rnd.nextInt(m_nScreenW), new_y = rnd.nextInt(m_nScreenH);
						//new_x = 100; new_y = 10;
						int ix = last_x, iy = last_y;
						while ((ix!=new_x || iy!=new_y) && m_bRunning) {
							
							//Log.e("XX", ""+ix+":"+iy+"  "+new_x+":"+new_y);
							if (new_x > ix) ix++; else if (new_x < ix) ix --;
							if (new_y > iy) iy++; else if (new_y < iy) iy --;


							Singleton.getInstance().m_CurService.Update(ix, iy, true);
							try {
								Thread.sleep(5);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}

						}
						last_x = new_x; last_y = new_y;
					}

		        }
		};
		t.start();*/

	}

	/*  @Override
   public void onTouch(final ControlTouchEvent event) {
        int action = event.getAction();
        Dbg.d("onTouch");
        if (mGameState == GameState.FINISHED_SHOW_IMAGE) {
            Dbg.d("onTouch mGameState == GameState.FINISHED_SHOW_IMAGE, wait for menu");
            return;
        } else if (mGameState == GameState.FINISHED_SHOW_MENU) {
            if (action == Control.Intents.TOUCH_ACTION_PRESS) {
                // Redraw screen with pressed state for one button
                if (sActionButton1Rect.contains(event.getX(), event.getY())) {
                    mPressedActionImageId = R.id.image_game_id;
                    mPressedActionDrawableId = R.drawable.image_game_icn_pressed;
                    mPressedButtonRect = sActionButton1Rect;
                } else if (sActionButton2Rect.contains(event.getX(), event.getY())) {
                    mPressedActionImageId = R.id.number_game_id;
                    mPressedActionDrawableId = R.drawable.number_game_icn_pressed;
                    mPressedButtonRect = sActionButton2Rect;
                } else if (sActionButton3Rect.contains(event.getX(), event.getY())) {
                    mPressedActionImageId = R.id.share_icon;
                    mPressedActionDrawableId = R.drawable.share_icn_pressed;
                    mPressedButtonRect = sActionButton3Rect;
                } else {
                    mPressedActionImageId = 0;
                    mPressedActionDrawableId = 0;
                    mPressedButtonRect = null;
                }
                mHandler.post(mDrawResult);
            } else if (action == Control.Intents.TOUCH_ACTION_RELEASE) {
                if (mPressedButtonRect == sActionButton1Rect) {
                    if (sActionButton1Rect.contains(event.getX(), event.getY())) {
                        // Pressed and released on new image button
                        Dbg.d("New image button pressed, starting new image game");
                        mGameType = GameType.IMAGE;
                        startNewGame();
                    } else {
                        // Pressed new image button but released elsewhere
                        // Redraw buttons
                        mPressedActionImageId = 0;
                        mPressedActionDrawableId = 0;
                        mHandler.post(mDrawResult);
                    }
                } else if (mPressedButtonRect == sActionButton2Rect) {
                    if (sActionButton2Rect.contains(event.getX(), event.getY())) {
                        // Pressed and released on new number button
                        Dbg.d("New number button pressed, starting new number game");
                        mGameType = GameType.NUMBERS;
                        startNewGame();
                    } else {
                        // Pressed new number button but released elsewhere
                        // Redraw buttons
                        mPressedActionImageId = 0;
                        mPressedActionDrawableId = 0;
                        mHandler.post(mDrawResult);
                    }
                } else if (mPressedButtonRect == sActionButton3Rect) {
                    if (sActionButton3Rect.contains(event.getX(), event.getY())) {
                        // Pressed and released on share button
                        Dbg.d("Share button pressed, sending intent");
                        // Send share intent with result
                        Intent resultIntent = new Intent(android.content.Intent.ACTION_SEND);
                        resultIntent.setType("text/plain");
                        resultIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                                mContext.getText(R.string.share_title));
                        StringBuilder shareText = new StringBuilder();
                        shareText.append(String.format(mContext.getString(R.string.share_text),
                                mNumberOfMoves));
                        shareText.append("\n\n");
                        shareText.append(mContext.getString(R.string.share_link));
                        Dbg.d("shareText: " + shareText.toString());
                        resultIntent.putExtra(android.content.Intent.EXTRA_TEXT,
                                shareText.toString());

                        Intent shareIntent = Intent.createChooser(resultIntent,
                                mContext.getText(R.string.share_result_title));
                        shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(shareIntent);                      
                    } else {
                        // Pressed share button but released elsewhere
                        // Do nothing
                    }
                    // Always redraw buttons
                    mPressedActionImageId = 0;
                    mPressedActionDrawableId = 0;
                    mHandler.post(mDrawResult);
                }
            }
        } else if (mGameState == GameState.PLAYING) {
            if (action == Control.Intents.TOUCH_ACTION_LONGPRESS) {
                // Show action menu
                mLongPressed = true;
                startVibrator(50, 0, 1);
                mPressedActionImageId = 0;
                mPressedActionDrawableId = 0;
                mHandler.post(mDrawActionScreen);
                mGameState = GameState.ACTION_MENU;

                //Mouse Overlay
                DisplayMetrics metrics = new DisplayMetrics();
        		try {
        			WindowManager winMgr = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE) ;
        	       	winMgr.getDefaultDisplay().getMetrics(metrics);
        	       	m_nScreenW = winMgr.getDefaultDisplay().getWidth();
        	       	m_nScreenH = winMgr.getDefaultDisplay().getHeight();
        		}
        		catch (Exception e) { //default to a HVGA 320x480 and let's hope for the best
        			e.printStackTrace();
        			m_nScreenW = 0;
        			m_nScreenH = 0;
        		} 

                cursorOn();

            } else if (action == Control.Intents.TOUCH_ACTION_PRESS) {
                mTilePressIndex = getTileIndex(event);
            } else if (action == Control.Intents.TOUCH_ACTION_RELEASE) {
                int tileReleaseIndex = getTileIndex(event);
                if (mTilePressIndex != tileReleaseIndex) {
                    Dbg.d("Skipping Pressed tile: " + mTilePressIndex + ", Release tile: "
                            + tileReleaseIndex);
                    mTilePressIndex = -1;
                    return;
                }
                mTilePressIndex = -1;
                // Find out if pressed tile can move
                boolean moveTileOk = tileCanMove(tileReleaseIndex);
                int middleTileIndex = -1;
                if (moveTileOk) {
                    // swap place with empty and moving tile
                    int movingTileIndex = tileReleaseIndex;
                    swapTiles(movingTileIndex, mEmptyTileIndex);
                    mEmptyTileIndex = movingTileIndex;
                } else {
                    // Check if the entire row can be moved
                    middleTileIndex = rowCanMove(tileReleaseIndex);
                    if (middleTileIndex != -1) {
                        // swap place with empty and middle tile
                        swapTiles(middleTileIndex, mEmptyTileIndex);
                        mEmptyTileIndex = middleTileIndex;
                        // swap place with empty and pressed tile
                        int movingTileIndex = tileReleaseIndex;
                        swapTiles(movingTileIndex, mEmptyTileIndex);
                        mEmptyTileIndex = movingTileIndex;
                    }
                }

                if (!moveTileOk && middleTileIndex == -1) {
                    Dbg.d("Tile " + tileReleaseIndex + " can not move.");
                    return;
                }

                // Check if finished
                if (gameFinished()) {
                    drawGameFinished();
                }
            }
        } else if (mGameState == GameState.ACTION_MENU) {
            if (action == Control.Intents.TOUCH_ACTION_PRESS) {
                // show pressed state
                mLongPressed = false;
                mPressedActionImageId = 0;
                mPressedActionDrawableId = 0;
                if (sActionButton1Rect.contains(event.getX(), event.getY())) {
                    mPressedActionImageId = R.id.return_id;
                    mPressedActionDrawableId = R.drawable.return_icn_pressed;
                    mPressedButtonRect = sActionButton1Rect;
                } else if (sActionButton2Rect.contains(event.getX(), event.getY())) {
                    mPressedActionImageId = R.id.image_game_id;
                    mPressedActionDrawableId = R.drawable.image_game_icn_pressed;
                    mPressedButtonRect = sActionButton2Rect;
                } else if (sActionButton3Rect.contains(event.getX(), event.getY())) {
                    mPressedActionImageId = R.id.number_game_id;
                    mPressedActionDrawableId = R.drawable.number_game_icn_pressed;
                    mPressedButtonRect = sActionButton3Rect;
                } else {
                    mPressedButtonRect = null;
                }
                mHandler.post(mDrawActionScreen);
            } else if (!mLongPressed && action == Control.Intents.TOUCH_ACTION_RELEASE) {
                // take action
                boolean hit = false;
                if (mPressedButtonRect == sActionButton1Rect
                        && sActionButton1Rect.contains(event.getX(), event.getY())) {
                    // Pressed and released on action 1
                    // Return to playing
                    mGameState = GameState.PLAYING;
                    getCurrentImage(true);
                    hit = true;
                } else if (mPressedButtonRect == sActionButton2Rect
                        && sActionButton2Rect.contains(event.getX(), event.getY())) {
                    // Pressed and released on action 2
                    // Start new image game
                    mGameType = GameType.IMAGE;
                    mTilePressIndex = -1;
                    startNewGame();
                    hit = true;
                } else if (mPressedButtonRect == sActionButton3Rect
                        && sActionButton3Rect.contains(event.getX(), event.getY())) {
                    // Pressed and released on action 3
                    // Start new number game
                    mGameType = GameType.NUMBERS;
                    mTilePressIndex = -1;
                    startNewGame();
                    hit = true;
                } else if (mPressedButtonRect == null
                        && !sActionButton1Rect.contains(event.getX(), event.getY())
                        && !sActionButton2Rect.contains(event.getX(), event.getY())
                        && !sActionButton3Rect.contains(event.getX(), event.getY())) {
                    // Pressed and released outside all three buttons
                    // Return to playing
                    mGameState = GameState.PLAYING;
                    getCurrentImage(true);
                    hit = true;
                }
                if (!hit) {
                    // Press and release not on same button, redraw buttons
                    mPressedActionImageId = 0;
                    mPressedActionDrawableId = 0;
                    mHandler.post(mDrawActionScreen);
                }
            }
        }
    }
	 */

	@Override
	public void onTouch(final ControlTouchEvent event) {
		int action = event.getAction();
		Dbg.d("onTouch");
		Log.d("Eight", "XYZZY: onTouch: "+action+" "+event.getX()+" "+event.getY());
		
		//long currTime = System.currentTimeMillis();
		long currTime = event.getTimeStamp();
		
		int ix = Singleton.getInstance().m_CurService.curr_x;
		int iy = Singleton.getInstance().m_CurService.curr_y;
		
		int currx = ix;
		int curry = iy;
		
		if (action == Control.Intents.TOUCH_ACTION_PRESS) {
			Log.d("Eight", "XYZZY: onTouch Press: "+event.getX()+" "+event.getY()+" "+currTime);
			pressedX = event.getX();
			pressedY = event.getY();
			pressedTime = currTime;
			longpress = false;
			// Redraw screen with pressed state for one button
			/*if (sActionButton1Rect.contains(event.getX(), event.getY())) {
	                    mPressedActionImageId = R.id.image_game_id;
	                    mPressedActionDrawableId = R.drawable.image_game_icn_pressed;
	                    mPressedButtonRect = sActionButton1Rect;	              
	                }*/
			// mHandler.post(mDrawResult);
		} else if (action == Control.Intents.TOUCH_ACTION_RELEASE) {
			
			longpress = false;
			int releasedX = event.getX();
			int releasedY = event.getY();
			Log.d("Eight", "XYZZY: rls "+pressedX+"  "+pressedY+ " "+releasedX+"  "+releasedY+ " "+currTime);
			long dragDuration = currTime-pressedTime;	
			int dispX = releasedX-pressedX;
			int dispY = releasedY-pressedY;
			Dbg.d("XYZZY: disp "+dispX+"  "+dispY);
			Log.d("Eight", "XYZZY: disp "+dispX+"  "+dispY);
			if (Math.abs(dispX) > 5||Math.abs(dispY) > 5) {

				//Drag
				double distance = Math.sqrt(Math.pow((dispX), 2)+ Math.pow((dispY), 2));
				double velocity = distance/dragDuration;
				Dbg.d("XYZZY: velo "+velocity);
				Log.d("Eight", "XYZZY: velo "+velocity);
				
				if (velocity < 128/1000){
					
					ix = ix+2*dispX;
					iy = iy+2*dispY;
					Log.d("Eight", "XYZZY: go "+ix);
				}
				else					
				{
					double k = (1000/128)*velocity;
					ix = ix+(int)(k*2*dispX);
					iy = iy+(int)(k*2*dispY);
				}
				if (ix<0)
				{
					ix = 0;
				}
				if (iy<0)
				{
					iy = 0;
				}
				if (ix>Singleton.getInstance().m_nScreenW-10)
				{
					ix = Singleton.getInstance().m_nScreenW-10;
				}
				if (iy>Singleton.getInstance().m_nScreenH-10)
				{
					iy = Singleton.getInstance().m_nScreenH-10;
				}
				
				if (dragMode == false)
				{
					Singleton.getInstance().m_CurService.Update(ix, iy, true);
				}
				else
				{
					try
					{
						Singleton.getInstance().out.writeBytes("input swipe "+ currx+" "+curry+" "+ix+" "+"iy\n");
						//out.writeBytes("input tap 500 600\n");

						//out.writeBytes("mv /system/file.old system/file.new\n");
						//out.writeBytes("exit\n");  
						Singleton.getInstance().out.flush();
						//process.waitFor();
					}
					catch (Exception e)
					{
						Dbg.d("KKFFF"+e.getMessage());
					}
				}
				
			} else if (dragDuration < 50){
				// Tap
				Dbg.d("XYZZY: tap "+ix+" "+iy+" "+" "+event.getX()+" "+event.getY());
				Log.d("Eight", "XYZZY: tap ");
				try
				{
					//Process process = Runtime.getRuntime().exec("su");
					//DataOutputStream out = new DataOutputStream(Singleton.getInstance().process.getOutputStream());
					//out.writeBytes("input swipe 500 600 10 600\n");
					
					Singleton.getInstance().out.writeBytes("input tap "+ix+" "+iy+"\n");

					//out.writeBytes("mv /system/file.old system/file.new\n");
					//out.writeBytes("exit\n");  
					Singleton.getInstance().out.flush();
					
					//process.waitFor();
				}
				catch (Exception e)
				{
					Dbg.d("KKFFF"+e.getMessage());
					Log.d("Eight", "XYZZY: Except ");
				}
				
				/*Intent resultIntent = new Intent(android.content.Intent.ACTION_SEND);
	                        resultIntent.setType("text/plain");
	                        resultIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
	                                mContext.getText(R.string.share_title));
	                        StringBuilder shareText = new StringBuilder();
	                        shareText.append(String.format(mContext.getString(R.string.share_text),
	                                mNumberOfMoves));
	                        shareText.append("\n\n");
	                        shareText.append(mContext.getString(R.string.share_link));
	                        Dbg.d("shareText: " + shareText.toString());
	                        resultIntent.putExtra(android.content.Intent.EXTRA_TEXT,
	                                shareText.toString());

	                        Intent shareIntent = Intent.createChooser(resultIntent,
	                                mContext.getText(R.string.share_result_title));
	                        shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	                        mContext.startActivity(shareIntent); */
			}
			
			pressedX = -1;
			pressedY = -1;
			pressedTime = -1;
		}                 
		
		else if (action == Control.Intents.TOUCH_ACTION_LONGPRESS) {
			// Show action menu
			if (longpress == false)
			{
				longpressStart = System.currentTimeMillis();
				longpress = true;
				
				
			}
			else if (System.currentTimeMillis()>longpressStart+500)
			{
				startVibrator(40, 0, 1);
				longpress = false;
				if (dragMode == false)
				{
					int size = mWidth*mHeight;
					int[] arr = new int[size];
					for (int i=0;i<size;i++)
					{
						arr[i] =Color.BLUE; 
					}
					Bitmap x = Bitmap.createBitmap(arr, mWidth, mHeight, BITMAP_CONFIG);
					showBitmap(x);
					dragMode = true;
				}
				else
				{
					int size = mWidth*mHeight;
					int[] arr = new int[size];
					for (int i=0;i<size;i++)
					{
						arr[i] =Color.WHITE; 
					}
					Bitmap x = Bitmap.createBitmap(arr, mWidth, mHeight, BITMAP_CONFIG);
					showBitmap(x);
					dragMode = false;
				}
				
			}
			
			mLongPressed = true;
			//startVibrator(50, 0, 1);
			mPressedActionImageId = 0;
			mPressedActionDrawableId = 0;
			// mHandler.post(mDrawActionScreen);
			mGameState = GameState.ACTION_MENU;

			

		} 	         
	}

	/**
	 * Check if tile can move, i.e., is adjacent with the empty tile.
	 *
	 * @param tilePressedIndex The tile index to check
	 * @return True if tile can be moved, false otherwise
	 */
	private boolean tileCanMove(int tilePressedIndex) {
		ArrayList<Integer> movableTileIndices = getMovableTileIndices(mEmptyTileIndex);
		return movableTileIndices.contains(tilePressedIndex);
	}

	/**
	 * Check if complete row can move.
	 *
	 * @param tilePressedIndex The tile that was pressed
	 * @return True if row can move, false otherwise
	 */
	private int rowCanMove(int tilePressedIndex) {
		// Check if row can move
		int middleTileIndex = -1;
		switch (tilePressedIndex) {
		case 1:
			if (mEmptyTileIndex == 3) {
				middleTileIndex = 2;
			} else if (mEmptyTileIndex == 7) {
				middleTileIndex = 4;
			}
			break;
		case 2:
			if (mEmptyTileIndex == 8) {
				middleTileIndex = 5;
			}
			break;
		case 3:
			if (mEmptyTileIndex == 1) {
				middleTileIndex = 2;
			} else if (mEmptyTileIndex == 9) {
				middleTileIndex = 6;
			}
			break;
		case 4:
			if (mEmptyTileIndex == 6) {
				middleTileIndex = 5;
			}
			break;
		case 5:
			break;
		case 6:
			if (mEmptyTileIndex == 4) {
				middleTileIndex = 5;
			}
			break;
		case 7:
			if (mEmptyTileIndex == 1) {
				middleTileIndex = 4;
			} else if (mEmptyTileIndex == 9) {
				middleTileIndex = 8;
			}
			break;
		case 8:
			if (mEmptyTileIndex == 2) {
				middleTileIndex = 5;
			}
			break;
		case 9:
			if (mEmptyTileIndex == 3) {
				middleTileIndex = 6;
			} else if (mEmptyTileIndex == 7) {
				middleTileIndex = 8;
			}
			break;
		default:
			break;
		}
		return middleTileIndex;
	}

	/**
	 * Get tile index for the coordinates in the event.
	 *
	 * @param event The touch event
	 * @return The tile index
	 */
	private int getTileIndex(ControlTouchEvent event) {
		int x = event.getX();
		int y = event.getY();
		int rowIndex = x / 42;
		int columnIndex = y / 42;
		return 1 + rowIndex + columnIndex * 3;
	}

	@Override
	public void onSwipe(int direction) {

		Log.d("Eight", "XYZZY: onSwipe: "+direction);
		int movementH = Singleton.getInstance().m_nScreenW/7;
		int movementW = Singleton.getInstance().m_nScreenW/7;
		int ix,iy;
		ix = Singleton.getInstance().m_CurService.curr_x;
		iy = Singleton.getInstance().m_CurService.curr_y;

		Dbg.d("onSwipe() " + direction);
		// change two tiles if direction is ok
		boolean directionOk = false;
		switch (direction) {
		case Control.Intents.SWIPE_DIRECTION_UP:
			if (dragMode == false)
			{
			if (iy <= movementH)
				iy = 1;
			else
				iy = iy-movementH;
			}
			else
			{
				try
				{
					Singleton.getInstance().out.writeBytes("input swipe "+ ix+" "+iy+" "+ix+" "+"0\n");
					//out.writeBytes("input tap 500 600\n");

					//out.writeBytes("mv /system/file.old system/file.new\n");
					//out.writeBytes("exit\n");  
					Singleton.getInstance().out.flush();
					//process.waitFor();
				}
				catch (Exception e)
				{
					Dbg.d("KKFFF"+e.getMessage());
				}
			}
			//directionOk = (mEmptyTileIndex < 7);
			break;
		case Control.Intents.SWIPE_DIRECTION_LEFT:
			if (dragMode == false)
			{
			if (ix <= movementW)
				ix = 1;
			else
				ix = ix-movementW;
			}
			else
			{
				try
				{
					Singleton.getInstance().out.writeBytes("input swipe "+ ix+" "+iy+" "+"0"+" "+iy+"\n");
					//out.writeBytes("input tap 500 600\n");

					//out.writeBytes("mv /system/file.old system/file.new\n");
					//out.writeBytes("exit\n");  
					Singleton.getInstance().out.flush();
					//process.waitFor();
				}
				catch (Exception e)
				{
					Dbg.d("KKFFF"+e.getMessage());
				}
			}
			//directionOk = (mEmptyTileIndex != 3 && mEmptyTileIndex != 6 && mEmptyTileIndex != 9);
			break;
		case Control.Intents.SWIPE_DIRECTION_DOWN:
			if (dragMode == false)
			{
				if (iy >= Singleton.getInstance().m_nScreenH-movementH)
					iy = Singleton.getInstance().m_nScreenH-1;
				else
					iy = iy+movementH;
			}
			else
			{
				try
				{
					Singleton.getInstance().out.writeBytes("input swipe "+ ix+" "+iy+" "+ix+" "+Singleton.getInstance().m_nScreenH+"\n");
					//out.writeBytes("input tap 500 600\n");

					//out.writeBytes("mv /system/file.old system/file.new\n");
					//out.writeBytes("exit\n");  
					Singleton.getInstance().out.flush();
					//process.waitFor();
				}
				catch (Exception e)
				{
					Dbg.d("KKFFF"+e.getMessage());
				}
			}
			//directionOk = (mEmptyTileIndex > 3);
			break;
		case Control.Intents.SWIPE_DIRECTION_RIGHT:
			
			if (dragMode == false)
			{
				if (ix >= Singleton.getInstance().m_nScreenW-movementW)
					ix = Singleton.getInstance().m_nScreenW-1;
				else
					ix = ix+movementW;
			}
			else
			{
				try
				{
					Singleton.getInstance().out.writeBytes("input swipe "+ ix+" "+iy+" "+Singleton.getInstance().m_nScreenW+" "+iy+"\n");
					//out.writeBytes("input tap 500 600\n");

					//out.writeBytes("mv /system/file.old system/file.new\n");
					//out.writeBytes("exit\n");  
					Singleton.getInstance().out.flush();
					//process.waitFor();
				}
				catch (Exception e)
				{
					Dbg.d("KKFFF"+e.getMessage());
				}
			}
			//directionOk = (mEmptyTileIndex != 1 && mEmptyTileIndex != 4 && mEmptyTileIndex != 7);
			break;
		default:
			break;
		}

		if (dragMode == false)
		{
			Singleton.getInstance().m_CurService.Update(ix, iy, true);
		}
		
			/*try
			{
				Process process = Runtime.getRuntime().exec("su");
				DataOutputStream out = new DataOutputStream(process.getOutputStream());
				out.writeBytes("input swipe 500 600 10 600\n");
				//out.writeBytes("input tap 500 600\n");

				//out.writeBytes("mv /system/file.old system/file.new\n");
				//out.writeBytes("exit\n");  
				out.flush();
				//process.waitFor();
			}
			catch (Exception e)
			{
				Dbg.d("KKFFF"+e.getMessage());
			}*/
		
		// if (!directionOk) {
		//   Dbg.d("direction not ok, " + mEmptyTileIndex);
		// return;
		//  }

		// swap place with empty and moving tile
		//  int movingTileIndex = getMovingTileIndex(direction);
		//  swapTiles(movingTileIndex, mEmptyTileIndex);
		//  Dbg.d("Changing empty index from " + mEmptyTileIndex + " to " + movingTileIndex);
		// mEmptyTileIndex = movingTileIndex;

		// Check if finished
		//  if (gameFinished()) {
          //  drawGameFinished();
        //}
	}

	/**
	 * Get the tile index for a swipe direction. Should not be called for bad
	 * swipe directions!
	 *
	 * @param direction The direction
	 * @return
	 */
	private int getMovingTileIndex(int direction) {
		int index;
		switch (direction) {
		case Control.Intents.SWIPE_DIRECTION_UP:
			index = mEmptyTileIndex + 3;
			break;
		case Control.Intents.SWIPE_DIRECTION_LEFT:
			index = mEmptyTileIndex + 1;
			break;
		case Control.Intents.SWIPE_DIRECTION_DOWN:
			index = mEmptyTileIndex - 3;
			break;
		case Control.Intents.SWIPE_DIRECTION_RIGHT:
			index = mEmptyTileIndex - 1;
			break;
		default:
			index = 0;
			break;
		}
		Dbg.d("getMovingTileIndex: " + index);
		return index;
	}

	/**
	 * Game is finished, show full image if in image game mode.
	 */
	private void drawGameFinished() {
		mGameState = GameState.FINISHED_SHOW_IMAGE;
		startVibrator(1000, 0, 1);

		if (mGameType == GameType.IMAGE) {
			// Draw full image
			showBitmap(mFullImage, 0, 0);
		}

		mPressedActionImageId = 0;
		mPressedActionDrawableId = 0;
		mHandler.postDelayed(mDrawResult, SHOW_SOLVED_IMAGE_TIME);
	}

	/**
	 * Show loading image screen.
	 */
	private void drawLoadingScreen() {
		// Draw loading
		LinearLayout root = new LinearLayout(mContext);
		root.setLayoutParams(new LayoutParams(mWidth, mHeight));

		LinearLayout sampleLayout = (LinearLayout)LinearLayout.inflate(mContext, R.layout.loading,
				root);
		sampleLayout.measure(mWidth, mHeight);
		sampleLayout
		.layout(0, 0, sampleLayout.getMeasuredWidth(), sampleLayout.getMeasuredHeight());
		// Create background bitmap for animation.
		Bitmap menu = Bitmap.createBitmap(mWidth, mHeight, BITMAP_CONFIG);
		// Set default density to avoid scaling.
		menu.setDensity(DisplayMetrics.DENSITY_DEFAULT);
		Canvas canvas = new Canvas(menu);
		
		sampleLayout.draw(canvas);

		showBitmap(menu);
	}

	/**
	 * Runnable that show screen when screen is long pressed. Includes new game
	 * and back button.
	 */
	private final Runnable mDrawActionScreen = new Runnable() {
		public void run() {
			// Update mCurrentImage to full screen image of current game
			mCurrentImage = getCurrentImage(false);

			LinearLayout root = new LinearLayout(mContext);
			root.setLayoutParams(new LayoutParams(mWidth, mHeight));

			LinearLayout actionLayout = (LinearLayout)LinearLayout.inflate(mContext,
					R.layout.action_menu_layout, root);

			// Set image resource if mPressedActionImageId != 0
			if (mPressedActionImageId != 0) {
				((ImageView)actionLayout.findViewById(mPressedActionImageId))
				.setImageResource(mPressedActionDrawableId);
			}

			actionLayout.measure(mWidth, mHeight);
			actionLayout.layout(0, 0, actionLayout.getMeasuredWidth(),
					actionLayout.getMeasuredHeight());
			// Create bitmap
			Bitmap menu = Bitmap.createBitmap(mWidth, mHeight, BITMAP_CONFIG);
			// Set default density to avoid scaling.
			menu.setDensity(DisplayMetrics.DENSITY_DEFAULT);
			Canvas canvas = new Canvas(menu);
			canvas.drawBitmap(mCurrentImage, null, new Rect(0, 0, mWidth, mHeight), null);
			actionLayout.draw(canvas);

			showBitmap(menu);
		}
	};

	/**
	 * Runnable that show screen when game is finished. Includes share and
	 * restart buttons.
	 */
	private final Runnable mDrawResult = new Runnable() {
		public void run() {
			Bitmap image = null;
			if (mGameType == GameType.IMAGE) {
				image = mFullImage;
			} else if (mGameType == GameType.NUMBERS) {
				// Update mCurrentImage to full screen image of current game
				image = getCurrentImage(false);
			}

			// Draw result menu
			LinearLayout root = new LinearLayout(mContext);
			root.setLayoutParams(new LayoutParams(mWidth, mHeight));

			LinearLayout finishedLayout = (LinearLayout)LinearLayout.inflate(mContext,
					R.layout.game_finished, root);
			((TextView)finishedLayout.findViewById(R.id.game_finished_moves_count_text))
			.setText(Integer.toString(mNumberOfMoves));

			// Set image resource if mPressedActionImageId != 0
			if (mPressedActionImageId != 0) {
				((ImageView)finishedLayout.findViewById(mPressedActionImageId))
				.setImageResource(mPressedActionDrawableId);
			}

			finishedLayout.measure(mWidth, mHeight);
			finishedLayout.layout(0, 0, finishedLayout.getMeasuredWidth(),
					finishedLayout.getMeasuredHeight());

			Bitmap bitmap = Bitmap.createBitmap(mWidth, mHeight, BITMAP_CONFIG);
			// Set default density to avoid scaling.
			bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
			Canvas canvas = new Canvas(bitmap);
			canvas.drawBitmap(image, null, new Rect(0, 0, mWidth, mHeight), null);
			finishedLayout.draw(canvas);
			showBitmap(bitmap, 0, 0);

			mGameState = GameState.FINISHED_SHOW_MENU;
		}
	};

	/**
	 * Swap position of two tiles.
	 *
	 * @param tileIndex1 The first tile
	 * @param tileIndex2 The second tile
	 */
	private void swapTiles(int tileIndex1, int tileIndex2) {
		GameTile tile1 = mGameTiles.get(tileIndex1 - 1);
		GameTile tile2 = mGameTiles.get(tileIndex2 - 1);

		// Create middle rect
		Rect unionRect = new Rect(tile1.tilePosition.frame);
		unionRect.union(tile2.tilePosition.frame);
		Bitmap bitmap = Bitmap.createBitmap(unionRect.width(), unionRect.height(), BITMAP_CONFIG);
		// Set the density to default to avoid scaling.
		bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawColor(Color.WHITE);

		// Draw moving tile in middle of bitmap
		int middleLeft = Math.abs(tile1.tilePosition.frame.left - tile2.tilePosition.frame.left) / 2;
		int middleTop = Math.abs(tile1.tilePosition.frame.top - tile2.tilePosition.frame.top) / 2;
		canvas.drawBitmap(tile1.bitmap, middleLeft, middleTop, null);
		showBitmap(bitmap, unionRect.left, unionRect.top);

		startVibrator(50, 0, 1);

		// Draw swapped position
		Rect tmpFrame = tile1.tilePosition.frame;
		tile1.tilePosition.frame = tile2.tilePosition.frame;
		tile2.tilePosition.frame = tmpFrame;

		// Draw tile1 at correct position on canvas
		// Set background color
		canvas.drawColor(Color.WHITE);
		int left = 0;
		int top = 0;
		if (tile1.tilePosition.frame.left > tile2.tilePosition.frame.left) {
			// tile 1 is to right of tile 2
			left = tile1.tilePosition.frame.left - tile2.tilePosition.frame.left;
		}
		if (tile1.tilePosition.frame.top > tile2.tilePosition.frame.top) {
			// tile 1 is below tile 2
			top = tile1.tilePosition.frame.top - tile2.tilePosition.frame.top;
		}
		canvas.drawBitmap(tile1.bitmap, left, top, null);
		showBitmap(bitmap, unionRect.left, unionRect.top);

		// Reorder game tiles in list
		mGameTiles.set(tileIndex2 - 1, tile1);
		mGameTiles.set(tileIndex1 - 1, tile2);

		mNumberOfMoves++;
	}

	/**
	 * Check if game is finished, i.e., all tiles are in order with the ninth
	 * being empty.
	 *
	 * @return True if game is finished
	 */
	private boolean gameFinished() {
		boolean finished = true;
		int reference = 1;
		for (GameTile gt : mGameTiles) {
			if (reference == 9) {
				finished = (gt.text == null);
				break;
			} else if (reference != gt.correctPosition) {
				finished = false;
				break;
			}
			reference++;
		}
		return finished;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.sonyericsson.extras.liveware.aef.util.control.ControlExtension#onPause
	 * ()
	 */
	@Override
	public void onPause() {
		Log.d("Eight", "XYZZY: Pau");
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.sonyericsson.extras.liveware.aef.util.control.ControlExtension#onResume
	 * ()
	 */
	@Override
	public void onResume() {
		if (mGameState == GameState.FINISHED_SHOW_IMAGE
				|| mGameState == GameState.FINISHED_SHOW_MENU) {
			// Redraw finished screen
			mPressedActionImageId = 0;
			mPressedActionDrawableId = 0;
			// mHandler.post(mDrawResult);
		} else if (mGameTiles == null) {
			//startNewGame();
		} else {
			//drawLoadingScreen();
			//getCurrentImage(true);
		}
		

			int size = mWidth*mHeight;
			int[] arr = new int[size];
			int color=0;
			if (dragMode == false)
				color =Color.WHITE;			
			else
				color = Color.BLUE;
			
			for (int i=0;i<size;i++)
			{
				arr[i] = color;
			}
			Bitmap x = Bitmap.createBitmap(arr, mWidth, mHeight, BITMAP_CONFIG);
			showBitmap(x);

			if(cursorOn == false)
			{
			//Mouse Overlay
			DisplayMetrics metrics = new DisplayMetrics();
			try {
				
				WindowManager winMgr = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE) ;
				winMgr.getDefaultDisplay().getMetrics(metrics);
				Singleton.getInstance().m_nScreenW = winMgr.getDefaultDisplay().getWidth();
				Singleton.getInstance().m_nScreenH = winMgr.getDefaultDisplay().getHeight();
				
			}
			catch (Exception e) { //default to a HVGA 320x480 and let's hope for the best
				e.printStackTrace();
				Singleton.getInstance().m_nScreenW = 0;
				Singleton.getInstance().m_nScreenH = 0;
				Log.d("Eight", "XYZZY: Excepti ");
			}
			cursorOn();
			cursorOn = true;
		}
		Log.d("Eight", "XYZZY: Res");
	}

	/**
	 * Init the 9 tile position objects.
	 *
	 * @param tilePositions The tile positions
	 */
	private void initTilePositions(TilePosition... tilePositions) {
		mTilePositions = new ArrayList<TilePosition>(9);
		for (TilePosition tilePosition : tilePositions) {
			mTilePositions.add(tilePosition);
		}
	}

	/**
	 * Create a list of movable indices based on current state.
	 *
	 * @param emptyIndex The current empty tile index
	 * @param indices Empty list to put indices in
	 * @return The new empty tile index
	 */
	private ArrayList<Integer> getMovableTileIndices(int emptyIndex) {
		ArrayList<Integer> indices = new ArrayList<Integer>();
		switch (emptyIndex) {
		case 1:
			indices.add(2);
			indices.add(4);
			break;
		case 2:
			indices.add(1);
			indices.add(3);
			indices.add(5);
			break;
		case 3:
			indices.add(2);
			indices.add(6);
			break;
		case 4:
			indices.add(1);
			indices.add(5);
			indices.add(7);
			break;
		case 5:
			indices.add(2);
			indices.add(4);
			indices.add(6);
			indices.add(8);
			break;
		case 6:
			indices.add(3);
			indices.add(5);
			indices.add(9);
			break;
		case 7:
			indices.add(4);
			indices.add(8);
			break;
		case 8:
			indices.add(5);
			indices.add(7);
			indices.add(9);
			break;
		case 9:
			indices.add(6);
			indices.add(8);
			break;
		default:
			Dbg.w("empty tile index not 1-9!");
			break;
		}

		return indices;
	}

	/**
	 * Init the 9 tiles with index and bitmap, based on game type.
	 */
	private void initTiles() {
		mGameTiles = new ArrayList<GameTile>(9);
		// Force size to 9
		for (int i = 0; i < 9; i++) {
			mGameTiles.add(new GameTile());
		}

		int i = 1;
		for (TilePosition tp : mTilePositions) {
			GameTile gt = new GameTile();
			if (i != 9) {
				gt.correctPosition = i;
				gt.text = Integer.toString(i);
			}

			gt.tilePosition = tp;
			if (mGameType == GameType.NUMBERS) {
				setNumberTile(gt);
			} else {
				setImageTile(mCurrentImage, gt);
			}
			mGameTiles.set(tp.position - 1, gt);
			i++;
		}
	}

	/**
	 * Create number based bitmap for tile
	 *
	 * @param gt The tile
	 */
	private void setNumberTile(GameTile gt) {
		gt.bitmap = Bitmap.createBitmap(40, 40, BITMAP_CONFIG);
		// Set the density to default to avoid scaling.
		gt.bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);

		Canvas canvas = new Canvas(gt.bitmap);
		if (gt.text != null) {
			canvas.drawColor(mContext.getResources().getColor(R.color.color_number_tile_background));
			canvas.drawText(gt.text, 13, 29, mNumberTextPaint);
		} else {
			// Empty tile
			canvas.drawColor(Color.WHITE);
			mEmptyTileIndex = gt.tilePosition.position;
		}
	}

	/**
	 * Create image based bitmap for tile
	 *
	 * @param imageBitmap The bitmap
	 * @param gt The tile
	 */
	private void setImageTile(Bitmap imageBitmap, GameTile gt) {
		Dbg.d("game tilePosition: " + gt.tilePosition + ", correctPosition: " + gt.correctPosition
				+ ", text: " + gt.text);
		if (gt.text != null) {
			setTileBitmap(imageBitmap, gt);
			Dbg.d(gt.toString());
		} else {
			// Empty tile
			gt.bitmap = Bitmap.createBitmap(40, 40, BITMAP_CONFIG);
			// Set the density to default to avoid scaling.
			gt.bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);

			Canvas canvas = new Canvas(gt.bitmap);
			gt.bitmap = Bitmap.createBitmap(40, 40, BITMAP_CONFIG);
			// Set the density to default to avoid scaling.
			gt.bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
			canvas = new Canvas(gt.bitmap);
			canvas.drawColor(Color.WHITE);
			mEmptyTileIndex = gt.tilePosition.position;
		}
	}
}
