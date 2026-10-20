-- Allow several providers of the same type (e.g. two vLLM servers, or an Ollama box
-- next to a vLLM cluster). Fresh installs get this from V003; run this on existing databases.
USE ai_panel_channel;

ALTER TABLE provider_config DROP INDEX uk_type;
ALTER TABLE provider_config ADD INDEX idx_type (type);
