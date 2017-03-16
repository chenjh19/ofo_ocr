% function carIdentify ()
clear all;
clc

[Image_ID] = csvread ('Plate_Index.csv',0,0,[0,0,0,0]);

for i =1:length (Image_ID)
filename=[int2str(Image_ID(i)),'.jpg'];
filename
img = imread (['Plate_Image\',filename]);

%��˹
a = imgaussfilt (img, 3);

%�ҶȻ�
a = rgb2gray (a);

%sobel��Ե���
a = edge (a, 'Sobel');
% imshow (a);

%���ղ���
% �����㣺ȥ����С����������
% �����㣺����������ֵ�Ĺ�����
se = strel ('rectangle',[7 27]); 
a = imclose (a,se);

a = imopen (a,se);

se = strel ('rectangle',[7 95]); 
a = imclose (a,se);

se = strel ('rectangle',[11 3]); 
a = imopen (a,se);

imwrite (a,'imout.jpg');

%�������е���Ӿ���(��������MATLAB������̳)
[l,m] =  bwlabel (a,8);
status = regionprops (l,'BoundingBox');
figure(10);
imshow (img);
hold on;
for j = 1:m
    rectangle ('position', status(j).BoundingBox, 'edgecolor', 'r');
end
hold off;
frame = getframe;
rec = frame2im(frame);
imwrite(rec,['S3_Rectangle_Image\',filename])

%������ӽ���ͼ��
for k = 1:m
%��߾��x����<200�ģ�����
    if status(k).BoundingBox(1) < 200
        status(k).BoundingBox(1)
        continue
%����ߴ�����200�ģ�����
    elseif status(k).BoundingBox(3) < 200
        status(k).BoundingBox(3)
        continue
    elseif status(k).BoundingBox(4) < 50
        status(k).BoundingBox(4)
        continue
    else
        i2 = imcrop (img,status(k).BoundingBox);
        imwrite (i2,['S4_Crop_Image\',int2str(Image_ID(i)),'_',int2str(k),'.jpg']);
    end
end
end

