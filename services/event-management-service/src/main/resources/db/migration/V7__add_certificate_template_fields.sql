ALTER TABLE certificate_eligibility_criteria 
ADD COLUMN template_image VARCHAR(255),
ADD COLUMN name_pos_x INT,
ADD COLUMN name_pos_y INT,
ADD COLUMN name_font_size INT,
ADD COLUMN name_font_color VARCHAR(50);
