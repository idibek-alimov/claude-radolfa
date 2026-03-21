"use client";

import { useEditor, EditorContent } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import { List, ListOrdered } from "lucide-react";

interface RichTextEditorProps {
  initialContent: string;
  onChange: (html: string) => void;
  maxLength?: number;
}

export function RichTextEditor({
  initialContent,
  onChange,
  maxLength = 5000,
}: RichTextEditorProps) {
  const editor = useEditor({
    extensions: [StarterKit],
    content: initialContent,
    onUpdate: ({ editor }) => {
      if (maxLength && editor.getText().length > maxLength) {
        editor.commands.undo();
        return;
      }
      onChange(editor.getHTML());
    },
  });

  const charCount = editor ? editor.getText().length : 0;
  const isWarning = charCount >= maxLength - 500;

  return (
    <div className="rounded-md border border-input bg-transparent text-sm shadow-sm focus-within:ring-1 focus-within:ring-ring">
      <div className="flex gap-1 border-b border-input px-2 py-1">
        <button
          type="button"
          aria-label="Bold"
          data-active={editor?.isActive("bold")}
          onClick={() => editor?.chain().focus().toggleBold().run()}
          className="px-2 py-1 rounded data-[active=true]:bg-muted"
        >
          <span className="font-bold">B</span>
        </button>
        <button
          type="button"
          aria-label="Italic"
          data-active={editor?.isActive("italic")}
          onClick={() => editor?.chain().focus().toggleItalic().run()}
          className="px-2 py-1 rounded data-[active=true]:bg-muted"
        >
          <span className="italic">I</span>
        </button>
        <button
          type="button"
          aria-label="Bullet list"
          data-active={editor?.isActive("bulletList")}
          onClick={() => editor?.chain().focus().toggleBulletList().run()}
          className="px-2 py-1 rounded data-[active=true]:bg-muted"
        >
          <List className="h-4 w-4" />
        </button>
        <button
          type="button"
          aria-label="Ordered list"
          data-active={editor?.isActive("orderedList")}
          onClick={() => editor?.chain().focus().toggleOrderedList().run()}
          className="px-2 py-1 rounded data-[active=true]:bg-muted"
        >
          <ListOrdered className="h-4 w-4" />
        </button>
      </div>
      <EditorContent editor={editor} className="px-3 py-2 min-h-[120px] text-sm" />
      <p
        className={`text-xs text-right px-3 pb-1 ${
          isWarning ? "text-destructive" : "text-muted-foreground"
        }`}
      >
        {charCount} / {maxLength}
      </p>
    </div>
  );
}
