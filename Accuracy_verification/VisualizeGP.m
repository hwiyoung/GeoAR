clc
close all
clearvars

fid = fopen(fullfile(pwd,'GP_ÆíÁý.txt'), 'r');
C1 = textscan(fid, '%f %f %f %f', 'CommentStyle','#');
fclose(fid);

gp = cell2mat(C1);
no_gp = size(gp,1);

figure
plot3(gp(:,2), gp(:,3), gp(:,4), 'r^','LineWidth',2);
% for n = 1:no_gp
%     h = text(gp(n,2), gp(n,3), gp(n,4), sprintf('%d',gp(n,1)));
%     set(h, 'Color', 'k', 'VerticalAlignment', 'top', 'HorizontalAlignment', 'left');
% end
view(3)
grid on, axis equal
xlabel('X'), ylabel('Y'), zlabel('Z')