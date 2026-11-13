import { useEffect, useRef, useState } from "react";
import { Eraser, PanelLeft, SendHorizontal } from "lucide-react";
import Message from "./Message.jsx";

const SUGGESTIONS = [
  "Summarize this document in 5 points",
  "What are the key terms or definitions?",
  "List any dates, numbers or deadlines mentioned",
];

export default function Chat({
  messages,
  loading,
  totalDocs,
  selectedCount,
  onSend,
  onClear,
  onOpenSidebar,
}) {
  const [text, setText] = useState("");
  const endRef = useRef(null);
  const inputRef = useRef(null);

  const canAsk = totalDocs > 0 && selectedCount > 0;

  // keep the newest message in view
  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [messages, loading]);

  // grow the text box with its content
  useEffect(() => {
    const el = inputRef.current;
    if (!el) return;
    el.style.height = "auto";
    el.style.height = `${Math.min(el.scrollHeight, 160)}px`;
  }, [text]);

  const submit = (value) => {
    const question = value.trim();
    if (!question || loading || !canAsk) return;
    setText("");
    onSend(question);
  };

  const onKeyDown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      submit(text);
    }
  };

  let scope = "No documents yet";
  if (totalDocs > 0) {
    scope =
      selectedCount === 0
        ? "Select a document to search"
        : `Searching ${selectedCount} of ${totalDocs} ${totalDocs === 1 ? "document" : "documents"}`;
  }

  let placeholder = "Ask a question about your PDFs…";
  if (totalDocs === 0) placeholder = "Upload a PDF to start asking questions";
  else if (selectedCount === 0) placeholder = "Select at least one document on the left";

  return (
    <main className="chat">
      <header className="chat-head">
        <button className="icon-btn drawer-open" onClick={onOpenSidebar} aria-label="Open documents">
          <PanelLeft size={20} />
        </button>
        <div className="chat-head-text">
          <h2>Chat</h2>
          <p>{scope}</p>
        </div>
        {messages.length > 0 && (
          <button className="ghost-btn" onClick={onClear}>
            <Eraser size={16} /> Clear chat
          </button>
        )}
      </header>

      <div className="thread">
        <div className="thread-inner">
          {messages.length === 0 && (
            <section className="empty">
              <h2>{totalDocs === 0 ? "Upload a PDF, then ask it questions." : "What do you want to know?"}</h2>
              <p>
                {totalDocs === 0
                  ? "Drop a file on the left. Pagewise reads it, and every answer points back to the page it came from."
                  : "Ask in English or Hindi. Every answer lists the pages it was found on."}
              </p>
              {canAsk && (
                <div className="suggestions">
                  {SUGGESTIONS.map((s) => (
                    <button key={s} onClick={() => submit(s)}>
                      {s}
                    </button>
                  ))}
                </div>
              )}
            </section>
          )}

          {messages.map((m) => (
            <Message key={m.id} message={m} />
          ))}

          {loading && (
            <div className="msg msg-assistant typing" aria-live="polite">
              <span className="dot" />
              <span className="dot" />
              <span className="dot" />
              <span className="sr-only">Looking through your documents</span>
            </div>
          )}
          <div ref={endRef} />
        </div>
      </div>

      <form
        className="composer"
        onSubmit={(e) => {
          e.preventDefault();
          submit(text);
        }}
      >
        <textarea
          ref={inputRef}
          rows={1}
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={onKeyDown}
          placeholder={placeholder}
          disabled={!canAsk}
          aria-label="Your question"
        />
        <button type="submit" className="send" disabled={!canAsk || loading || !text.trim()} aria-label="Send question">
          <SendHorizontal size={20} />
        </button>
      </form>
    </main>
  );
}
