package com.vanniktech.emoji.material;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.Nullable;
import com.vanniktech.emoji.EmojiLayoutFactory;

public class MaterialEmojiLayoutFactory extends EmojiLayoutFactory {
  public MaterialEmojiLayoutFactory(@Nullable LayoutInflater.Factory2 delegate) {
    super(delegate);
  }

  @Override public View onCreateView(@Nullable View parent, String name, Context context, AttributeSet attrs) {
    if ("Button".equals(name)) {
      return new EmojiMaterialButton(context, attrs);
    } else if ("CheckBox".equals(name)) {
      return new EmojiMaterialCheckBox(context, attrs);
    } else if ("RadioButton".equals(name)) {
      return new EmojiMaterialRadioButton(context, attrs);
    } else {
      return super.onCreateView(parent, name, context, attrs);
    }
  }
}
